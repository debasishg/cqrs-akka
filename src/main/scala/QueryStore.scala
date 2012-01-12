package net.debasishg.domain.trade.model

import akka.actor.{Actor, FSM}
import FSM._
import TradeModel._

case object QueryAllTrades

// QueryStore modeled as an actor
class TradeQueryStore extends Actor {
  private var trades = new collection.immutable.TreeSet[Trade]()(Ordering.by(_.refNo))

  def receive = {
    case Transition(_, _, _) => 
    case CurrentState(_, _) => 

    case QueryAllTrades =>
      sender ! trades.toList

    case trade: Trade =>
      trades += trades.find(_ == trade).map(_ => trade).getOrElse(trade)
  }
}

