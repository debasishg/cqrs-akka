package net.debasishg.domain.trade.model

import akka.actor.{Actor, FSM}
import akka.util.duration._

import TradeModel._

class TradeLifecycle(trade: Trade, log: EventLogg) extends Actor with FSM[TradeState, Trade] {
  import FSM._

  startWith(Created, trade)

  when(Created) {
    case Event(AddValueDate, data) =>
      log.appendAsync(Created, Some(data), AddValueDate)
      goto(ValueDateAdded) using addValueDate(data) forMax(5 seconds)
  }

  when(ValueDateAdded) {
    case Event(EnrichTrade, data) =>
      log.appendAsync(ValueDateAdded, None,  EnrichTrade)
      goto(Enriched) using enrichTrade(data) forMax(5 seconds)
  }

  when(Enriched) {
    case Event(SendOutContractNote, data) =>
      log.appendAsync(Enriched, Some(data),  SendOutContractNote)
      stop
  }

  // onTransition {
    // case Created -> ValueDateAdded => println("**** changing from created to valuedateadded")
  // }

  initialize
}
      
