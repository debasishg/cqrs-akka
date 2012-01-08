package net.debasishg.domain.trade.model

import akka.dispatch._
import akka.util.duration._
import akka.actor.{Actor, ActorRef, Props, ActorSystem}
import Actor._

abstract class InMemoryEventLog(as: ActorSystem) extends EventLogg {
  lazy val logger = as.actorOf(Props[Logger], name = "event-logger")

  // def iterator: Iterator[EventLogEntry]
  // def iterator(fromEntryId: Long): Iterator[EventLogEntry]
  // def appendAsync(event: Event): Future[EventLogEntry]
  // def append(event: Event): EventLogEntry = Await.result(appendAsync(event), 1 second)

  case class LogEvent(event: Event)
  case class GetEntries()

  class Logger extends Actor {
    private var events = List.empty[EventLogEntry]
    def receive = {
      case LogEvent(event) =>
        val entry = EventLogEntry(InMemoryEventLog.nextId(), event)
        
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
