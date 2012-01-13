package net.debasishg.domain.trade
package serialization

import java.util.Date
import sjson.json.Format
import sjson.json.DefaultProtocol._
import dispatch.json._
import sjson.json.JsonSerialization._

import event.{Event, State, EventLogEntry}
import model.TradeModel._

object Serialization {
  implicit val EventFormat: Format[Event] = new Format[Event] {
    def reads(json: JsValue): Event = json match {
      case JsString("NewTrade") => NewTrade
      case JsString("EnrichTrade") => EnrichTrade
      case JsString("AddValueDate") => AddValueDate
      case JsString("SendOutContractNote") => SendOutContractNote
      case _ => sys.error("Invalid Event")
    }
    def writes(a: Event): JsValue = a match {
      case NewTrade => JsString("NewTrade")
      case EnrichTrade => JsString("EnrichTrade")
      case AddValueDate => JsString("AddValueDate")
      case SendOutContractNote => JsString("SendOutContractNote")
    }
  }

  implicit val StateFormat: Format[State] = new Format[State] {
    def reads(json: JsValue): State = json match {
      case JsString("Created") => Created
      case JsString("Enriched") => Enriched
      case JsString("ValueDateAdded") => ValueDateAdded
      case _ => sys.error("Invalid State")
    }
    def writes(a: State): JsValue = a match {
      case Created => JsString("Created")
      case Enriched => JsString("Enriched")
      case ValueDateAdded => JsString("ValueDateAdded")
    }
  }

  implicit val TaxFeeIdFormat: Format[TaxFeeId] = new Format[TaxFeeId] {
    def reads(json: JsValue): TaxFeeId = json match {
      case JsString("TradeTax") => TradeTax
      case JsString("Commission") => Commission
      case JsString("VAT") => VAT
      case _ => sys.error("Invalid TaxFeeId")
    }
    def writes(a: TaxFeeId): JsValue = a match {
      case TradeTax => JsString("TradeTax")
      case Commission => JsString("Commission")
      case VAT => JsString("VAT")
    }
  }

  implicit val MarketFormat: Format[Market] = new Format[Market] {
    def reads(json: JsValue): Market = json match {
      case JsString("HongKong") => HongKong
      case JsString("Singapore") => Singapore
      case JsString("NewYork") => NewYork
      case JsString("Tokyo") => Tokyo
      case JsString("Other") => Other
      case _ => sys.error("Invalid State")
    }
    def writes(a: Market): JsValue = a match {
      case HongKong => JsString("HongKong")
      case Singapore => JsString("Singapore")
      case NewYork => JsString("NewYork")
      case Tokyo => JsString("Tokyo")
      case Other => JsString("Other")
    }
  }

  implicit object BigDecimalFormat extends Format[BigDecimal] {
    def writes(o: BigDecimal) = JsValue.apply(o)
    def reads(json: JsValue) = json match {
      case JsNumber(n) => n
      case _ => throw new RuntimeException("BigDecimal expected")
    }
  }

  implicit object DateFormat extends Format[Date] {
    def writes(o: Date) = JsValue.apply(o.getTime.toString)
    def reads(json: JsValue) = json match {
      case JsString(s) => sjson.json.Util.mkDate(s)
      case _ => throw new RuntimeException("Date expected")
    }
  }

  implicit val TradeFormat: Format[Trade] = new Format[Trade] {
    def writes(t: Trade): JsValue = 
      JsObject(List(
        (tojson("account").asInstanceOf[JsString], tojson(t.account)),
        (tojson("instrument").asInstanceOf[JsString], tojson(t.instrument)),
        (tojson("refNo").asInstanceOf[JsString], tojson(t.refNo)),
        (tojson("market").asInstanceOf[JsString], tojson(t.market)),
        (tojson("unitPrice").asInstanceOf[JsString], tojson(t.unitPrice)),
        (tojson("quantity").asInstanceOf[JsString], tojson(t.quantity)),
        (tojson("tradeDate").asInstanceOf[JsString], tojson(t.tradeDate)),
        (tojson("valueDate").asInstanceOf[JsString], tojson(t.valueDate)),
        (tojson("taxFees").asInstanceOf[JsString], tojson(t.taxFees)),
        (tojson("netAmount").asInstanceOf[JsString], tojson(t.netAmount)) ))

    def reads(json: JsValue): Trade = json match {
      case JsObject(m) =>
        Trade(fromjson[Account](m(JsString("account"))), 
              fromjson[Instrument](m(JsString("instrument"))), 
              fromjson[String](m(JsString("refNo"))), 
              fromjson[Market](m(JsString("market"))), 
              fromjson[BigDecimal](m(JsString("unitPrice"))), 
              fromjson[BigDecimal](m(JsString("quantity"))), 
              fromjson[Date](m(JsString("tradeDate"))), 
              fromjson[Option[Date]](m(JsString("valueDate"))), 
              fromjson[Option[List[(TaxFeeId, BigDecimal)]]](m(JsString("taxFees"))), 
              fromjson[Option[BigDecimal]](m(JsString("netAmount"))))
      case _ => throw new RuntimeException("JsObject expected")
    }
  }

  implicit val EventLogEntryFormat: Format[EventLogEntry] = new Format[EventLogEntry] {
    def writes(e: EventLogEntry): JsValue = 
      JsObject(List(
        (tojson("entryId").asInstanceOf[JsString], tojson(e.entryId)), 
        (tojson("objectId").asInstanceOf[JsString], tojson(e.objectId)), 
        (tojson("inState").asInstanceOf[JsString], tojson(e.inState)),
        e.withData match {
          case Some(t) => t match {
            case trd: Trade => (tojson("withData").asInstanceOf[JsString], tojson(trd))
            case _ => sys.error("invalid trade data")
          }
          case _ => (tojson("withData").asInstanceOf[JsString], tojson("$notrade$"))
        },
        (tojson("event").asInstanceOf[JsString], tojson(e.event)) ))

     def reads(json: JsValue): EventLogEntry = json match {
       case JsObject(m) =>
         EventLogEntry(fromjson[Long](m(JsString("entryId"))), 
                       fromjson[String](m(JsString("objectId"))), 
                       fromjson[State](m(JsString("inState"))),
                       m(JsString("withData")) match {
                         case JsString("$notrade$") => None
                         case t => Some(fromjson[Trade](t))
                       },
                       fromjson[Event](m(JsString("event"))))
       case _ => throw new RuntimeException("JsObject expected")
     }
  }
}

object Util {
  def serializeEventLogEntry(e: EventLogEntry)(implicit f: Format[EventLogEntry])
    = tobinary(e)
  def deSerializeEventLogEntry(bytes: Array[Byte])(implicit f: Format[EventLogEntry])
    = frombinary[EventLogEntry](bytes)
}
