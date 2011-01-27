package net.debasishg.domain.trade.dsl

/**
 * Created by IntelliJ IDEA.
 * User: debasish
 * Date: 24/12/10
 * Time: 10:49 PM
 * To change this template use File | Settings | File Templates.
 */

trait ExecutionModel {this: RefModel =>
  case class Execution(account: Account, instrument: Instrument, refNo: String, market: Market,
    unitPrice: BigDecimal, quantity: BigDecimal)
}