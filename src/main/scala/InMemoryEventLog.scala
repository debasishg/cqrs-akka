package net.debasishg.domain.trade
package event

import akka.dispatch._
import akka.util.Timeout
import akka.util.duration._
import akka.actor.{Actor, ActorRef, Props, ActorSystem}
import Actor._

class InMemoryEventLog(as: ActorSystem) extends EventLog {
  val loggerActorName = "event-logger"

  // need a pinned dispatcher to maintain order of log entries
  val dispatcher = as.dispatcherFactory.newPinnedDispatcher(loggerActorName)

  lazy val logger = as.actorOf(Props(new Logger).withDispatcher(dispatcher), name = loggerActorName)
  implicit val timeout = as.settings.ActorTimeout

  def iterator = iterator(0L)

  def iterator(fromEntryId: Long) =
    getEntries.drop(fromEntryId.toInt).iterator

  def appendAsync(id: String, state: State, data: Option[Any], event: Event): Future[EventLogEntry] =
    (logger ? LogEvent(id, state, data, event)).asInstanceOf[Future[EventLogEntry]]

  def getEntries: List[EventLogEntry] = {
    val future = logger ? GetEntries()
    Await.result(future, timeout.duration).asInstanceOf[List[EventLogEntry]]
  }

  case class LogEvent(objectId: String, state: State, data: Option[Any], event: Event)
  case class GetEntries()

  class Logger extends Actor {
    private var entries = List.empty[EventLogEntry]
    def receive = {
      case LogEvent(id, state, data, event) =>
        val entry = EventLogEntry(InMemoryEventLog.nextId(), id, state, data, event)
        entries = entry :: entries
        sender ! entry

      case GetEntries() =>
        sender ! entries.reverse
    }
  }
}

object InMemoryEventLog {
  var current = 0L
  def nextId() = {
    current = current + 1
    current
  }
}
