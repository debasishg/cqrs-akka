package net.debasishg.domain.trade.service

/**
 * Created by IntelliJ IDEA.
 * User: debasish
 * Date: 23/12/10
 * Time: 10:53 PM
 * To change this template use File | Settings | File Templates.
 */

import org.scalatest.Spec
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith

@RunWith(classOf[JUnitRunner])
class TradingServiceSpec extends Spec with ShouldMatchers {

  import scalaz._
  import Scalaz._

  import akka.actor.{Actor, ActorRef}
  import Actor._

  describe("trading service") {
    it("should create and operate on multiple trades") {
      import TradingService._
      import java.util.Calendar
      import net.debasishg.domain.trade.model.TradeModel._

      val trd1 = newTrade("a-123", "google", "r-123", HongKong, 12.25, 200).toOption.get
      val trd2 = newTrade("a-125", "ibm", "r-125", Tokyo, 22.25, 250).toOption.get

      // start command store and query store actors
      val qs: ActorRef = actorOf(new QueryStore)
      implicit val cs: ActorRef = actorOf(new CommandStore(qs))
      qs.start
      cs.start

      // domain logic
      val t2 = doEnrichTrade(doAddValueDate(trd1))
      val t4 = doEnrichTrade(doAddValueDate(trd2))

      // get snapshot to see if command store has proper logs of events
      val es = (cs !! Snapshot).as[Set[Trade]].getOrElse(throw new Exception("cannot get trades from command store"))
      es.size should equal(2)
      es.toList should equal(List(t2, t4))
      es.foreach{ trade => // for each trade value date - trade date should be 3
        val tdt = trade.tradeDate
        val vdt = trade.valueDate.get
        val ct = Calendar.getInstance
        ct.setTime(tdt)
        val vt = Calendar.getInstance
        vt.setTime(vdt)
        vt.get(Calendar.DAY_OF_MONTH) - ct.get(Calendar.DAY_OF_MONTH) should equal(3)
      }

      // query store should have the appropriate models
      val ts = (qs !! QuerySnapshot).as[List[Trade]].getOrElse(throw new Exception("cannot get trades from query store"))
      ts should equal(es.toList)

      qs.stop
      cs.stop
    }
  }
}
