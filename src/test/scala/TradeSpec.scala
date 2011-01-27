package net.debasishg.domain.trade

import org.scalatest.Spec
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith

@RunWith(classOf[JUnitRunner])
class TradeSpec extends Spec with ShouldMatchers {

  import scalaz._
  import Scalaz._

  val t1 = Map("account" -> "a-123", "instrument" -> "google", "refNo" -> "r-123", "market" -> "HongKong", "unitPrice" -> "12.25", "quantity" -> "200")
  val t2 = Map("account" -> "b-123", "instrument" -> "ibm", "refNo" -> "r-234", "market" -> "Singapore", "unitPrice" -> "15.25", "quantity" -> "400")

  describe("trades") {
    it("should create and operate on 1 trade") {
      import Trades._

      val trd1 = makeTrade(t1)
      trd1 should equal(Some(Trade("a-123","google","r-123",HongKong,12.25,200)))
      principal(trd1.get) should equal(2450.0)

      (trd1 map forTrade) should equal(Some((Trade("a-123","google","r-123",HongKong,12.25,200),Some(List(TradeTax, Commission)))))

      ((trd1 map forTrade) map taxFees) should equal(Some((Trade("a-123","google","r-123",HongKong,12.25,200),List((TradeTax,490.000), (Commission,367.5000)))))

      (((trd1 map forTrade) map taxFees) map enrichWith) should equal(Some(RichTrade(Trade("a-123","google","r-123",HongKong,12.25,200),Map(TradeTax -> 490.000, Commission -> 367.5000))))

      ((((trd1 map forTrade) ∘ taxFees) ∘ enrichWith) ∘ netAmount) should equal(Some(3307.5000))
      (trd1 ∘ forTrade ∘ taxFees ∘ enrichWith ∘ netAmount) should equal(Some(3307.5000))
    }

    it("should create and operate on multiple trades") {
      import Trades._

      val trd1 = makeTrade(t1)
      val trd2 = makeTrade(t2)

      // test sequence
      List(trd1, trd2).sequence should equal(Some(List(Trade("a-123","google","r-123",HongKong,12.25,200), Trade("b-123","ibm","r-234",Singapore,15.25,400))))

      ((List(trd1, trd2)) ∘∘ forTrade) should equal(List(Some((Trade("a-123","google","r-123",HongKong,12.25,200),Some(List(TradeTax, Commission)))), Some((Trade("b-123","ibm","r-234",Singapore,15.25,400),Some(List(TradeTax, Commission, VAT))))))

      (((List(trd1, trd2)) ∘∘ forTrade) ∘∘ taxFees) should equal(List(Some((Trade("a-123","google","r-123",HongKong,12.25,200),List((TradeTax,490.000), (Commission,367.5000)))), Some((Trade("b-123","ibm","r-234",Singapore,15.25,400),List((TradeTax,1220.000), (Commission,915.0000), (VAT,610.000))))))

      ((((List(trd1, trd2)) ∘∘ forTrade) ∘∘ taxFees) ∘∘ enrichWith) should equal(List(Some(RichTrade(Trade("a-123","google","r-123",HongKong,12.25,200),Map(TradeTax -> 490.000, Commission -> 367.5000))), Some(RichTrade(Trade("b-123","ibm","r-234",Singapore,15.25,400),Map(TradeTax -> 1220.000, Commission -> 915.0000, VAT -> 610.000)))))

      (((((List(trd1, trd2)) ∘∘ forTrade) ∘∘ taxFees) ∘∘ enrichWith) ∘∘ netAmount) should equal(List(Some(3307.5000), Some(8845.0000)))
    }
  }
}
