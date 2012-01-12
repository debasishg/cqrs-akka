package net.debasishg.domain.trade.model

import akka.actor.{Actor, FSM}
import akka.util.duration._
import akka.routing.Listeners

import TradeModel._

class TradeLifecycle(trade: Trade, log: Option[EventLog]) extends Actor with FSM[TradeState, Trade] with Listeners {
  import FSM._

  startWith(Created, trade)

  when(Created) {
    case m@Event(AddValueDate, data) =>
      log.map(_.appendAsync(data.refNo, Created, Some(data), AddValueDate))
      val trd = addValueDate(data)
      notifyListeners(trd) // will change in Akka 2.0M3
      goto(ValueDateAdded) using trd forMax(5 seconds)
  }

  when(ValueDateAdded) {
    case m@Event(EnrichTrade, data) =>
      log.map(_.appendAsync(data.refNo, ValueDateAdded, None,  EnrichTrade))
      val trd = enrichTrade(data)
      notifyListeners(trd)
      goto(Enriched) using trd forMax(5 seconds)
  }

  when(Enriched) {
    case m@Event(SendOutContractNote, data) =>
      log.map(_.appendAsync(data.refNo, Enriched, None,  SendOutContractNote))
      sender ! data
      stop
  }

  // onTransition {
    // case Created -> ValueDateAdded => println("**** changing from created to valuedateadded")
  // }

  initialize
}
