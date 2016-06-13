package models

import play.api.libs.json.Json
import uk.gov.hmrc.domain.EmpRef

case class PayrollMonth(year: Int, month: Int)

object PayrollMonth {
  implicit val formats = Json.format[PayrollMonth]
}

case class LevyDeclaration(payrollMonth: PayrollMonth, amount: BigDecimal, submissionType: String, submissionDate: String)

object LevyDeclaration {
  implicit val formats = Json.format[LevyDeclaration]
}

case class LevyDeclarations(empref: EmpRef, levyAllowanceApplied: Boolean, declarations: Seq[LevyDeclaration])

object LevyDeclarations {
  implicit val formats = Json.format[LevyDeclarations]
}