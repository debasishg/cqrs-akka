package net.debasishg.domain.trade.model

import akka.dispatch._
import akka.util.Timeout
import akka.util.duration._
import akka.actor.{Actor, ActorRef, Props, ActorSystem}
import Actor._

class InMemoryEventLog(as: ActorSystem) extends EventLogg {
  lazy val logger = as.actorOf(Props(new Logger), name = "event-logger")
  implicit val timeout = as.settings.ActorTimeout

  def iterator = iterator(0L)

  def iterator(fromEntryId: Long) =
    getEntries.drop(fromEntryId.toInt).iterator

  def appendAsync(state: State, data: Option[Any], event: Event): Future[EventLogEntry] =
    (logger ? LogEvent(state, data, event)).asInstanceOf[Future[EventLogEntry]]

  def getEntries: List[EventLogEntry] = {
    val future = logger ? GetEntries()
    Await.result(future, timeout.duration).asInstanceOf[List[EventLogEntry]]
  }

  case class LogEvent(state: State, data: Option[Any], event: Event)
  case class GetEntries()

  class Logger extends Actor {
    private var entries = List.empty[EventLogEntry]
    def receive = {
      case LogEvent(state, data, event) =>
        val entry = EventLogEntry(InMemoryEventLog.nextId(), state, data, event)
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
