package net.debasishg.domain.trade.model

import TradeModel._
object WriterLog {
  type LOG = List[(Trade, TradeEvent)]
}

import WriterLog._

case class Writer[A](log: LOG, a: A) {
  def map[B](f: A => B): Writer[B] =
    Writer(log, f(a))

  def flatMap[B](f: A => Writer[B]): Writer[B] = {
    val Writer(log2, b) = f(a)
    Writer(log ::: log2 /* accumulate */, b)
  }
}

object Writer {
  implicit def LogUtilities[A](a: A) = new {
    def nolog =
      Writer(Nil /* empty */, a)

    def withlog(log: (Trade, TradeEvent)) =
      Writer(List(log), a)

    def withvaluelog(log: A => (Trade, TradeEvent)) =
      withlog(log(a))
  }
}
