package models

import play.api.mvc.PathBindable
import uk.gov.hmrc.domain.EmpRef

import scala.util.{Failure, Success, Try}

object EmprefBindable extends PathBindable[EmpRef] {
  override def bind(key: String, value: String): Either[String, EmpRef] = Try(EmpRef.fromIdentifiers(value)) match {
    case Success(e) => Right(e)
    case Failure(t) => Left(t.getMessage)
  }

  override def unbind(key: String, e: EmpRef): String = s"${e.taxOfficeNumber}/${e.taxOfficeReference}"
}

object PlayBindings {
  implicit val emprefBindable = EmprefBindable
}