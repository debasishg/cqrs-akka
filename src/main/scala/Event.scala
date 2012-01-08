package net.debasishg.domain.trade.model

import akka.dispatch._
import akka.util.duration._

trait Event
trait State

case class EventLogEntry[T](entryId: Long, inState: State, withData: T, event: Event)

trait EventLogg extends Iterable[EventLogEntry] {
  def iterator: Iterator[EventLogEntry]
  def iterator(fromEntryId: Long): Iterator[EventLogEntry]
  def appendAsync(event: Event): Future[EventLogEntry]
  def append(event: Event): EventLogEntry = Await.result(appendAsync(event), 1 second)
}
