package net.debasishg.domain.trade.service

/**
 * Created by IntelliJ IDEA.
 * User: debasish
 * Date: 23/12/10
 * Time: 6:06 PM
 * To change this template use File | Settings | File Templates.
 */

import scalaz._
import Scalaz._

import java.util.{Date, Calendar}
import akka.actor.{Actor, ActorRef}
import akka.config.Supervision.{OneForOneStrategy,Permanent}
import Actor._
import akka.routing.{Listeners, Listen}

import net.debasishg.domain.trade.model._
import TradeModel._

// command events
// processed by CommandStore and forwarded to QueryStore
case class TradeEnriched(trade: Trade, closure: TradeEvent)
case class ValueDateAdded(trade: Trade, closure: TradeEvent)

case object Snapshot
case object QueryAllTrades

class TradingClient {
  val ts = Actor.registry.actorsFor(classOf[TradingServer]).head

  // create a trade : wraps the model method
  def newTrade(account: Account, instrument: Instrument, refNo: String, market: Market,
    unitPrice: BigDecimal, quantity: BigDecimal, tradeDate: Date = Calendar.getInstance.getTime) =
      makeTrade(account, instrument, refNo, market, unitPrice, quantity) // @tofix : additional args

  // refer To Mock a Mockingbird
  private[service] def kestrel[T](trade: T, proc: T => T)(effect: => Unit) = {
    val t = proc(trade)
    effect
    t
  }

  // enrich trade
  def doEnrichTrade(trade: Trade) = 
    kestrel(trade, enrichTrade) { 
      ts ! TradeEnriched(trade, enrichTrade)
    }

  // add value date
  def doAddValueDate(trade: Trade) = 
    kestrel(trade, addValueDate) { 
      ts ! ValueDateAdded(trade, addValueDate)
    }

  def getCommandSnapshot = 
    (ts !! Snapshot).as[Set[Trade]].getOrElse(throw new Exception("cannot get trades from trade server"))

  def getAllTrades =
    (ts !! QueryAllTrades).as[List[Trade]].getOrElse(throw new Exception("cannot get trades from trade server"))
}

// CommandStore modeled as an actor
// It also keeps a list of listeners to publish events to them
// Currently the only listener added is the QueryStore
class CommandStore extends Actor with Listeners {
  private var events = Map.empty[Trade, List[TradeEvent]]

  def receive = listenerManagement orElse {
    case m@TradeEnriched(trade, closure) => 
      events += ((trade, events.getOrElse(trade, List.empty[TradeEvent]) :+ closure))
      gossip(m)
    case m@ValueDateAdded(trade, closure) => 
      events += ((trade, events.getOrElse(trade, List.empty[TradeEvent]) :+ closure))
      gossip(m)
    case Snapshot => 
      self.reply(events.keys.map {trade =>
        events(trade).foldLeft(trade)((t, e) => e(t))
      })
  }
}

// QueryStore modeled as an actor
class QueryStore extends Actor {
  private var trades = new collection.immutable.TreeSet[Trade]()(Ordering.by(_.refNo))

  def receive = {
    case TradeEnriched(trade, closure) => 
      trades += trades.find(_ == trade).map(closure(_)).getOrElse(closure(trade))
    case ValueDateAdded(trade, closure) => 
      trades += trades.find(_ == trade).map(closure(_)).getOrElse(closure(trade))
    case QueryAllTrades =>
      self.reply(trades.toList)
  }
}

// Creates and links Storage
trait StoreFactory {this: Actor =>
  val queryStore = this.self.spawnLink[QueryStore] // starts and links QueryStore
  val commandStore = actorOf[CommandStore]
  this.self.link(commandStore)
  commandStore.start
  commandStore ! Listen(queryStore)
}

trait TradingServer extends Actor {
  self.faultHandler = OneForOneStrategy(List(classOf[Exception]),5, 5000)
  val commandStore: ActorRef
  val queryStore: ActorRef

  // actor message handler
  def receive = { 
    case m@TradeEnriched(trade, closure) => 
      commandStore forward m
    case m@ValueDateAdded(trade, closure) => 
      commandStore forward m
    case m@Snapshot => 
      commandStore forward m
    case m@QueryAllTrades =>
      queryStore forward m
  }

  override def postStop = {
    self.unlink(queryStore)
    self.unlink(commandStore)
    queryStore.stop
    commandStore.stop
  }
}

class TradingService extends TradingServer with StoreFactory {
}
