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
import Actor._

object TradingService {
  import net.debasishg.domain.trade.model._
  import TradeModel._

  // service methods

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
  def doEnrichTrade(trade: Trade)(implicit commandStore: ActorRef) = 
    kestrel(trade, enrichTrade) { 
      commandStore ! TradeEnriched(trade, enrichTrade)
    }

  // add value date
  def doAddValueDate(trade: Trade)(implicit commandStore: ActorRef) = 
    kestrel(trade, addValueDate) { 
      commandStore ! ValueDateAdded(trade, addValueDate)
    }

  type TradeEvent = (Trade => Trade)

  // command events
  // processed by CommandStore and forwarded to QueryStore
  case class TradeEnriched(trade: Trade, closure: TradeEvent)
  case class ValueDateAdded(trade: Trade, closure: TradeEvent)

  case object Snapshot

  // CommandStore modeled as an actor
  class CommandStore(qryStore: ActorRef) extends Actor {
    private var events = Map.empty[Trade, List[TradeEvent]]

    def receive = {
      case m@TradeEnriched(trade, closure) => 
        events += ((trade, events.getOrElse(trade, List.empty[TradeEvent]) :+ closure))
        qryStore forward m
      case m@ValueDateAdded(trade, closure) => 
        events += ((trade, events.getOrElse(trade, List.empty[TradeEvent]) :+ closure))
        qryStore forward m
      case Snapshot => 
        self.reply(events.keys.map {trade =>
          events(trade).foldLeft(trade)((t, e) => e(t))
        })
    }
  }

  case object QuerySnapshot

  // QueryStore modeled as an actor
  class QueryStore extends Actor {
    private var trades = new collection.immutable.TreeSet[Trade]()(Ordering.by(_.refNo))

    def receive = {
      case TradeEnriched(trade, closure) => 
        trades += trades.find(_ == trade).map(closure(_)).getOrElse(closure(trade))
      case ValueDateAdded(trade, closure) => 
        trades += trades.find(_ == trade).map(closure(_)).getOrElse(closure(trade))
      case QuerySnapshot =>
        self.reply(trades.toList)
    }
  }
}
