package net.debasishg.domain.trade.model

/**
 * Created by IntelliJ IDEA.
 * User: debasish
 * Date: 24/12/10
 * Time: 10:36 PM
 * To change this template use File | Settings | File Templates.
 */

import java.util.{Date, Calendar}
import scalaz._
import Scalaz._

trait OrderModel {this: RefModel =>
  case class LineItem(ins: Instrument, qty: BigDecimal, price: BigDecimal)
  case class Order(no: String, date: Date, customer: Customer, items: List[LineItem])

  def fromClientOrders(cos: List[Map[String, String]]) = {
    cos map {co =>
      val ins = co("instrument").split("-")
      val lineItems = ins map {in =>
        val arr = in.split("/")
        LineItem(arr(0), BigDecimal(arr(1)), BigDecimal(arr(2)))
      }
      Order(co("no"), Calendar.getInstance.getTime, co("customer"), lineItems.toList)
    }
  }

  def validItems(items: List[LineItem]): Validation[String, List[LineItem]] = {
    if (items.isEmpty) "Cannot have an empty list of line items for order".fail
    else items.success
  }

  def validDate(date: Date): Validation[String, Date] = {
    date.success
  }

  // using Validation as an applicative
  // can be combined to accumulate exceptions
  def makeOrder(no: String, date: Date, customer: Customer, items: List[LineItem]) =
    (validItems(items).liftFailNel |@|
      validDate(date).liftFailNel) { (i, d) => Order(no, d, customer, i) }
}
