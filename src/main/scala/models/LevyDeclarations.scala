package models

import org.joda.time.format.DateTimeFormat
import org.joda.time.{LocalDate, LocalDateTime}
import play.api.libs.json._
import uk.gov.hmrc.domain.EmpRef

case class PayrollMonth(year: Int, month: Int)

object PayrollMonth {
  implicit val formats = Json.format[PayrollMonth]
}

case class EnglishFraction(fraction: BigDecimal, calculatedAt: LocalDate)

object EnglishFraction {
  implicit val formats = Json.format[EnglishFraction]
}

case class PayrollPeriod(year: String, month: Int)

object PayrollPeriod {
  implicit val formats = Json.format[PayrollPeriod]
}

case class LevyDeclaration(id: Long,
                           submissionTime: LocalDateTime,
                           dateCeased: Option[LocalDate] = None,
                           inactiveFrom: Option[LocalDate] = None,
                           inactiveTo: Option[LocalDate] = None,
                           payrollPeriod: Option[PayrollPeriod] = None,
                           levyDueYTD: Option[BigDecimal] = None,
                           levyAllowanceForFullYear: Option[BigDecimal] = None,
                           noPaymentForPeriod: Option[Boolean] = None)


object LevyDeclaration {
  implicit val ldtFormats = new Format[LocalDateTime] {
    val fmt = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSS")

    override def reads(json: JsValue): JsResult[LocalDateTime] = implicitly[Reads[JsString]].reads(json).map { js =>
      fmt.parseDateTime(js.value).toLocalDateTime
    }

    override def writes(o: LocalDateTime): JsValue = JsString(fmt.print(o))
  }

  implicit val formats = Json.format[LevyDeclaration]
}

case class LevyDeclarations(empref: EmpRef, declarations: Seq[LevyDeclaration])

object LevyDeclarations {
  implicit val formats = Json.format[LevyDeclarations]
}

case class Emprefs(emprefs: Seq[String])

object Emprefs {
  implicit val formats = Json.format[Emprefs]
}