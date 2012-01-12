package net.debasishg.domain.trade.model

import TradeModel._
object EventLg {
  type LOG = List[(Trade, TradeEvent)]
}

import EventLg._

case class EventLogger[A](log: LOG, a: A) {
  def map[B](f: A => B): EventLogger[B] =
    EventLogger(log, f(a))

  def flatMap[B](f: A => EventLogger[B]): EventLogger[B] = {
    val EventLogger(log2, b) = f(a)
    EventLogger(log ::: log2 /* accumulate */, b)
  }
}

object EventLogger {
  implicit def LogUtilities[A](a: A) = new {
    def nolog =
      EventLogger(Nil /* empty */, a)

    def withlog(log: (Trade, TradeEvent)) =
      EventLogger(List(log), a)

    def withvaluelog(log: A => (Trade, TradeEvent)) =
      withlog(log(a))
  }
}

