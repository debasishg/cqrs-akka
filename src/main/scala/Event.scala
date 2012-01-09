package net.debasishg.domain.trade.model

import akka.dispatch._
import akka.util.duration._

trait Event
trait State

case class EventLogEntry(entryId: Long, inState: State, withData: Option[Any], event: Event)

trait EventLogg extends Iterable[EventLogEntry] {
  def iterator: Iterator[EventLogEntry]
  def iterator(fromEntryId: Long): Iterator[EventLogEntry]
  def appendAsync(state: State, data: Option[Any], event: Event): Future[EventLogEntry]
  def append(state: State, data: Option[Any], event: Event): EventLogEntry = 
    Await.result(appendAsync(state, data, event), 1 second)
}
