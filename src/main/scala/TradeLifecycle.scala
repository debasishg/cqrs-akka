package net.debasishg.domain.trade.model

import akka.actor.{Actor, FSM}
import akka.util.duration._

import TradeModel._

class TradeLifecycle(trade: Trade) extends Actor with FSM[TradeState, Trade] {
  import FSM._

  startWith(Created, trade)

  when(Created) {
    case Event(AddValueDate, data) =>
      println("in state created: " + data)
      goto(ValueDateAdded) using addValueDate(data) forMax(5 seconds)
  }

  when(ValueDateAdded) {
    case Event(EnrichTrade, data) =>
      println("in state valueDateAdded: " + data)
      goto(Enriched) using enrichTrade(data) forMax(5 seconds)
  }

  when(Enriched) {
    case Event(SendOutContractNote, data) =>
      println("in state enriched: " + data)
      stop
  }

  onTransition {
    case Created -> ValueDateAdded => println("**** changing from created to valuedateadded")
  }

  initialize
}
      
