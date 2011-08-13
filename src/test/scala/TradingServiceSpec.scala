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

  import java.util.Calendar
  import net.debasishg.domain.trade.model._
  import net.debasishg.domain.trade.model.TradeModel._
  import akka.actor.{Actor, ActorRef}
  import Actor._

  describe("trading service that logs events asynchronously") {
    it("should create and operate on multiple trades") {
      val ts = actorOf[TradingService].start
      val c = new TradingClient
      import c._

      // make trades
      val trd1 = newTrade("a-123", "google", "r-123", HongKong, 12.25, 200).toOption.get
      val trd2 = newTrade("a-125", "ibm", "r-125", Tokyo, 22.25, 250).toOption.get

      // domain logic
      val t1 = doEnrichTrade(doAddValueDate(trd1))
      val t2 = doEnrichTrade(doAddValueDate(trd2))

      // build command snapshot to get latest states of trades
      val es = getCommandSnapshot.get
      es.size should equal(2)
      es.toList should equal(List(t1, t2))

      // fetch from query store should give the same set
      val trades = getAllTrades.get
      trades.size should equal(2)
      trades should equal(es.toList)
      ts.stop
    }
  }

  // make bulk trades
  // 1000 trades with ransdom account / instrument and other data
  def makeTrades = {
    val securities = Vector("google", "ibm", "cisco", "oracle")
    val markets = Vector(HongKong, Singapore, NewYork, Tokyo)
    def giveUnitPrice = BigDecimal(new util.Random().nextInt(100) + 1)
    def giveQuantity = BigDecimal(new util.Random().nextInt(500) + 1)
    def giveAccount = "a-" + new util.Random().nextInt(500).toString
    def giveRefNo = "r-" + new util.Random().nextInt(500).toString

    val c = new TradingClient
    import c._
    for(i <- 1 to 1000)
      yield newTrade(giveAccount, 
               securities(new util.Random().nextInt(4)),
               giveRefNo + i.toString,
               markets(new util.Random().nextInt(4)),
               giveUnitPrice,
               giveQuantity).toOption.get
  }

  describe("trading service that does computation with futures") {
    it("should compute tax/fees over all trades in nonblocking mode") {
      val ts = actorOf[TradingService].start
      val c = new TradingClient
      import c._

      // make trades
      val trds = 
        List(
          newTrade("a-123", "google", "r-123", HongKong, 12.25, 200).toOption.get,
          newTrade("a-124", "ibm", "r-124", Tokyo, 22.25, 250).toOption.get,
          newTrade("a-125", "cisco", "r-125", NewYork, 20.25, 150).toOption.get,
          newTrade("a-126", "ibm", "r-127", Singapore, 22.25, 250).toOption.get)

      // domain logic
      val enrichedTrades = trds.map(trd => doEnrichTrade(doAddValueDate(trd)))

      // send closing message
      doClose.get should equal("closed")

      // sum all tax/fees
      sumTaxFees(getAllTrades.get) should equal(6370.625)
      ts.stop
    }
  }

  describe("trading service that does computation with futures in volumes") {
    it("should compute tax/fees over all trades in nonblocking mode") {
      val ts = actorOf[TradingService].start
      val c = new TradingClient
      import c._

      // make trades
      val trds = makeTrades
      println(trds.size)

      // domain logic
      val enrichedTrades = trds.map(trd => doEnrichTrade(doAddValueDate(trd)))
      println(enrichedTrades.size)

      // send closing message
      doClose.get should equal("closed")
      // Thread.sleep(2000)
      println(getAllTrades.get.size)

      // sum all tax/fees
      val start = System.nanoTime
      println(sumTaxFees(getAllTrades.get))
      val end = System.nanoTime
      println("elapsed: " + (end - start))
      ts.stop
    }
  }

  describe("trading service that does computation in volumes") {
    it("should compute tax/fees over all trades in nonblocking mode") {
      val ts = actorOf[TradingService].start
      val c = new TradingClient
      import c._

      // make trades
      val trds = makeTrades
      println(trds.size)

      // domain logic
      val enrichedTrades = trds.map(trd => doEnrichTrade(doAddValueDate(trd)))
      println(enrichedTrades.size)

      // send closing message
      doClose.get should equal("closed")
      // Thread.sleep(3000)

      val start = System.nanoTime
      val trades = getAllTrades.get
      println(trades.size)

      val taxFees = 
      trades.map {trade =>
        // Thread.sleep(10)
        trade.taxFees
             .map(_.map(_._2).foldLeft(BigDecimal(0))(_ + _))
             .getOrElse(sys.error("cannot get tax/fees"))
      }

      println(taxFees.sum)
      val end = System.nanoTime
      println("elapsed: " + (end - start))
      ts.stop
    }
  }

  describe("trading service that logs events using Writer monad") {
    it("should create and operate on a trade") {
      import EventLogger._

      val trd = makeTrade("a-123", "google", "r-123", HongKong, 12.25, 200).toOption.get

      val r = for {
        t1 <- enrichTrade(trd) withlog (trd, enrichTrade)
        t2 <- addValueDate(t1) withlog (trd, addValueDate)
      } yield t2

      val m = r.log.groupBy(_._1)
      val x =
        m.keys.map {t =>
          m(t).map(_._2).foldLeft(t)((a,e) => e(a))
        }
      x.size should equal(1)
      x.head.taxFees.get.size should equal(2) 
      x.head.netAmount.get should equal(3307.5000)
    }

    it("should create and operate on multiple trades") {
      import EventLogger._

      val trds = List(
        makeTrade("a-123", "google", "r-123", HongKong, 12.25, 200).toOption.get,
        makeTrade("a-125", "ibm", "r-125", Tokyo, 22.25, 250).toOption.get
      )

      val t =
      trds map {trd =>
        val r = for {
          t1 <- enrichTrade(trd) withlog (trd, enrichTrade)
          t2 <- addValueDate(t1) withlog (trd, addValueDate)
        } yield t2

        val m = r.log.groupBy(_._1)
        val x =
          m.keys.map {t =>
            m(t).map(_._2).foldLeft(t)((a,e) => e(a))
          }
        x.head
      }
      t.size should equal(2)
    }
  }

  describe("trading service that logs events using State monad") {
    it("should create and operate on a trade") {
      import scalaz._
      import Scalaz._

      val trd = makeTrade("a-123", "google", "r-123", HongKong, 12.25, 200).toOption.get

      val x =
        for {
          _ <- init[Trade]
          _ <- modify((t: Trade) => refNoLens.set(t, "XXX-123"))
          u <- modify((t: Trade) => taxFeeLens.set(t, some(List((TradeTax, 102.25), (Commission, 25.65)))))
        } yield(u)

      x ~> trd == trd.copy(refNo = "XXX-123")
    }
  }
}
