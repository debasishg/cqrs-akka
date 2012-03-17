package net.debasishg.domain.trade
package service

import event.{EventLog, EventLogEntry}
import akka.actor.{ActorRef, ActorSystem, Props}
import akka.dispatch._
import akka.pattern.ask
import akka.util.{Timeout, Duration}
import akka.util.duration._
import model.TradeModel._

trait TradeSnapshot {
  def doSnapshot(log: EventLog, system: ActorSystem): List[Trade] = {
    // implicit val timeout = system.settings.ActorTimeout
    implicit val timeout = Timeout(20 seconds)
    val l = new collection.mutable.ListBuffer[Trade]
    var mar = Map.empty[String, ActorRef]
    log.foreach {entry =>
      val EventLogEntry(id, oid, state, d, ev) = entry
      if (state.toString == "Created") {
        mar += ((oid, system.actorOf(Props(new TradeLifecycle(d.asInstanceOf[Option[Trade]].get, timeout.duration, None)), name = "tlc-" + oid)))
        mar(oid) ! ev
      } else if (state.toString == "Enriched") {
        val future = mar(oid) ? SendOutContractNote
        l += Await.result(future, timeout.duration).asInstanceOf[Trade]
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
