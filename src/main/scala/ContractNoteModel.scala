package net.debasishg.domain.trade.dsl

/**
 * Created by IntelliJ IDEA.
 * User: debasish
 * Date: 25/12/10
 * Time: 1:46 PM
 * To change this template use File | Settings | File Templates.
 */

import scalaz._
import Scalaz._

trait ContractNoteModel {this: TradeModel =>
  val isAddressDefined = true
  case class ContractNote(trade: Trade)

//  def makeContractNote(trade: Option[Trade]): Validation[String, ContractNote] =
//    if (trade.isEmpty) "Trade not found".fail
//    else if (isAddressDefined) ContractNote(trade.get).success else "Address not defined".fail

  def makeContractNote(trade: Trade): Validation[String, ContractNote] =
    if (isAddressDefined) ContractNote(trade).success else "Address not defined".fail
}