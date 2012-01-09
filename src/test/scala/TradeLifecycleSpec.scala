package net.debasishg.domain.trade.model

import org.scalatest.{Spec, BeforeAndAfterAll}
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith

@RunWith(classOf[JUnitRunner])
class TradeLifecycleSpec extends Spec with ShouldMatchers with BeforeAndAfterAll {
  import java.util.Calendar
  import akka.actor.{Actor, ActorRef, Props, ActorSystem}
  import akka.util.Timeout
  import akka.util.duration._
  import Actor._
  import TradeModel._

  val system = ActorSystem("TradingSystem")
  override def afterAll = { system.shutdown() }

  describe("trade lifecycle") {
    it("should work") {

      // make trades
      val trds = 
        List(
          Trade("a-123", "google", "r-123", HongKong, 12.25, 200),
          Trade("a-124", "ibm", "r-124", Tokyo, 22.25, 250),
          Trade("a-125", "cisco", "r-125", NewYork, 20.25, 150),
          Trade("a-126", "ibm", "r-127", Singapore, 22.25, 250))

      val log = new InMemoryEventLog(system)
      trds.foreach {trd =>
        val tlc = system.actorOf(Props(new TradeLifecycle(trd, log)))
        tlc ! AddValueDate
        tlc ! EnrichTrade
        tlc ! SendOutContractNote
      }
      Thread.sleep(1000)
      log.foreach(println)
    }
  }
}
