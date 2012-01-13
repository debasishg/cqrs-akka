package net.debasishg.domain.trade
package event

import util.Serialization
import akka.dispatch._
import akka.util.Timeout
import akka.util.duration._
import akka.actor.{Actor, ActorRef, Props, ActorSystem}
import Actor._
import com.redis._
import com.redis.serialization._

class RedisEventLog(clients: RedisClientPool, as: ActorSystem) extends EventLog {
  val loggerActorName = "redis-event-logger"

  // need a pinned dispatcher to maintain order of log entries
  val dispatcher = as.dispatcherFactory.newPinnedDispatcher(loggerActorName)

  lazy val logger = as.actorOf(Props(new Logger(clients)).withDispatcher(dispatcher), name = loggerActorName)
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

  class Logger(clients: RedisClientPool) extends Actor {
    def receive = {
      case LogEvent(id, state, data, event) =>
        val entry = EventLogEntry(RedisEventLog.nextId(), id, state, data, event)
        clients.withClient {client =>
          client.lpush(RedisEventLog.logName, Serialization.serialize(entry))
        }
        sender ! entry

      case GetEntries() =>
        import Parse.Implicits.parseByteArray
        val entries = 
          clients.withClient {client =>
            client.lrange[Array[Byte]](RedisEventLog.logName, 0, -1)
          }
        val ren = entries.map(_.map(e => Serialization.deserialize(e.get))).getOrElse(List.empty[EventLogEntry]).reverse
        println("**************************")
        ren.foreach(println)
        println("**************************")
        sender ! ren
    }
  }
}

object RedisEventLog {
  var current = 0L
  def logName = "events"
  def nextId() = {
    current = current + 1
    current
  }
}
