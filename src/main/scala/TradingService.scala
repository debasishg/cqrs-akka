package net.debasishg.domain.trade.service

import java.util.{Date, Calendar}
import akka.actor.{Actor, ActorRef, Props, ActorSystem}
import Actor._
import akka.routing.{Listeners, Listen}
import akka.dispatch.{Future, Dispatchers, Await}
import akka.util.Timeout
import akka.util.duration._

import net.debasishg.domain.trade.model._
import TradeModel._

// command events
// processed by EventStore and forwarded to QueryStore
case class TradeEnriched(trade: Trade, closure: TradeEvent)
case class ValueDateAdded(trade: Trade, closure: TradeEvent)

case object Closing
case object Snapshot
case object QueryAllTrades

class TradingClient(ts: ActorRef, asys: ActorSystem) {
  implicit val timeout = Timeout(12 millis)
  implicit val dispatcher = asys.dispatcherFactory.lookup("trading-dispatcher")

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
  // side-effect: actor message send non-blocking
  def doEnrichTrade(trade: Trade) = 
    kestrel(trade, enrichTrade) { 
      ts ! TradeEnriched(trade, enrichTrade)
    }

  // add value date
  // side-effect: actor message send non-blocking
  def doAddValueDate(trade: Trade) = 
    kestrel(trade, addValueDate) { 
      ts ! ValueDateAdded(trade, addValueDate)
    }

  def doClose = (ts ? Closing).mapTo[String]

  // non-blocking: returns a Future
  def getCommandSnapshot = (ts ? Snapshot).mapTo[Set[Trade]]

  // non-blocking: returns a Future
  def getAllTrades = (ts ? QueryAllTrades).mapTo[List[Trade]]

  // non-blocking and composition of futures
  // a function to operate on every trade in the list of trades
  def onTrades[T](trades: List[Trade])(fn: Trade => T) = { 
    Future.traverse(trades) {trade =>
      Future { fn(trade) }
    }
  }

  def sumTaxFees(trades: List[Trade]) = { 
    println("size = " + trades.size)
    val maybeTaxFees =
      onTrades[BigDecimal](trades) {trade =>
        trade.taxFees
             .map(_.map(_._2).foldLeft(BigDecimal(0))(_ + _))
             .getOrElse(sys.error("cannot get tax/fees"))
      }
    Await.result(maybeTaxFees, 1 second).sum
  }
}

// EventStore modeled as an actor
// It also keeps a list of listeners to publish events to them
// Currently the only listener added is the QueryStore
class EventStore extends Actor with Listeners {
  private var events = Map.empty[Trade, List[TradeEvent]]

  def receive = listenerManagement orElse {
    case m@TradeEnriched(trade, closure) => 
      events += ((trade, events.getOrElse(trade, List.empty[TradeEvent]) :+ closure))
      gossip(m)
    case m@ValueDateAdded(trade, closure) => 
      events += ((trade, events.getOrElse(trade, List.empty[TradeEvent]) :+ closure))
      gossip(m)
    case Snapshot => 
      sender ! (events.keys.map {trade => events(trade).foldLeft(trade)((t, e) => e(t))})
    case Closing =>
      sender ! "closed"
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
      sender ! trades.toList
  }
}

trait TradingServer extends Actor {
  lazy val eventStore = context.actorOf(Props[EventStore], name = "event-store")
  lazy val queryStore = context.actorOf(Props[QueryStore], name = "query-store")
  eventStore ! Listen(queryStore)

  // actor message handler
  def receive = { 
    case m@TradeEnriched(trade, closure) => 
      eventStore forward m
    case m@ValueDateAdded(trade, closure) => 
      eventStore forward m
    case m@Snapshot => 
      eventStore forward m
    case m@QueryAllTrades =>
      queryStore forward m
    case m@Closing =>
      eventStore forward m
  }

  override def postStop = {
    context.stop(queryStore)
    context.stop(eventStore)
  }
}

class TradingService extends TradingServer {
}
