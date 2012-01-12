package net.debasishg.domain.trade.model

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.dispatch._
import akka.util.duration._
import TradeModel._

trait Event
trait State

case class EventLogEntry(entryId: Long, objectId: String, inState: State, withData: Option[Any], event: Event)

trait EventLog extends Iterable[EventLogEntry] {
  def iterator: Iterator[EventLogEntry]
  def iterator(fromEntryId: Long): Iterator[EventLogEntry]
  def appendAsync(id: String, state: State, data: Option[Any], event: Event): Future[EventLogEntry]
  def append(id: String, state: State, data: Option[Any], event: Event): EventLogEntry = 
    Await.result(appendAsync(id, state, data, event), 1 second)
}

trait TradeSnapshot {
  def doSnapshot(log: EventLog, system: ActorSystem): List[Trade] = {
    implicit val timeout = system.settings.ActorTimeout
    val l = new collection.mutable.ListBuffer[Trade]
    var mar = Map.empty[String, ActorRef]
    log.foreach {entry =>
      val EventLogEntry(id, oid, state, d, ev) = entry
      if (state == Created) {
        mar += ((oid, system.actorOf(Props(new TradeLifecycle(d.asInstanceOf[Option[Trade]].get, None)), name = "tlc-" + oid)))
        mar(oid) ! ev
      } else if (state == Enriched) {
        val future = mar(oid) ? SendOutContractNote
        l += Await.result(future, system.settings.ActorTimeout.duration).asInstanceOf[Trade]
      } else {
        mar(oid) ! ev
      }
    }
    l.toList
  }
}

object TradeSnapshot extends TradeSnapshot {
  def snapshot(log: EventLog, system: ActorSystem) = doSnapshot(log, system)
}
