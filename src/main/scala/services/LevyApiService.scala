package services

import javax.inject.Inject

import com.google.inject.ImplementedBy
import models.{EmployerDetail, Emprefs, LevyDeclarations}
import play.api.Logger
import play.api.data.validation.ValidationError
import play.api.libs.json.{JsPath, Json}
import play.api.libs.ws.WSClient
import play.api.mvc.RequestHeader
import views.html.helper

import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[LevyApiImpl])
trait LevyApiService {
  def root(authToken: String)(implicit rh: RequestHeader): Future[Either[String, Emprefs]]

  def declarations(empref: String, authToken: String)(implicit rh: RequestHeader): Future[Either[String, LevyDeclarations]]

  def employerDetails(empref: String, authToken: String)(implicit rh: RequestHeader): Future[Either[String, EmployerDetail]]
}

class LevyApiImpl @Inject()(ws: WSClient)(implicit ec: ExecutionContext) extends LevyApiService {

  import ServiceConfig.config

  override def root(authToken: String)(implicit rh: RequestHeader): Future[Either[String, Emprefs]] = {
    val uri = config.api.baseURI + "/"

    ws.url(uri).withHeaders("Authorization" -> s"Bearer $authToken", "Accept" -> "application/vnd.hmrc.1.0+json").get.map { response =>
      response.status match {
        case 200 =>
          response.json.validate[Emprefs].fold(
            { (errs: Seq[(JsPath, Seq[ValidationError])]) =>
              formatErrs(errs).foreach(Logger.warn(_))
              Left(s"failed to unmarshall json")
            },
            emprefs => Right(emprefs)
          )

        case _ =>
          Logger.error(s"Got status ${response.status} calling levy api")
          Logger.error(uri)
          Logger.error(response.body)
          Left(s"Got status ${response.status} with body '${response.body}' when calling levy api")
      }
    }
  }

  def declarations(empref: String, authToken: String)(implicit rh: RequestHeader): Future[Either[String, LevyDeclarations]] = {
    val uri = config.api.baseURI + s"/${helper.urlEncode(empref)}/declarations"

    ws.url(uri).withHeaders("Authorization" -> s"Bearer $authToken", "Accept" -> "application/vnd.hmrc.1.0+json").get.map { response =>
      response.status match {
        case 200 =>
          response.json.validate[LevyDeclarations].fold(
            { (errs: Seq[(JsPath, Seq[ValidationError])]) =>
              formatErrs(errs).foreach(Logger.warn(_))
              Left(s"failed to unmarshall json")
            },
            decls => Right(decls)
          )

        case _ =>
          Logger.error(s"Got status ${response.status} calling levy api")
          Logger.error(uri)
          Logger.error(response.body)
          Left(s"Got status ${response.status} with body '${response.body}' when calling levy api")
      }
    }
  }

  case class HodName(nameLine1: Option[String] = None, nameLine2: Option[String] = None)

  case class DesignatoryDetailsData(name: Option[HodName] = None)

  case class DesignatoryDetails(employer: Option[DesignatoryDetailsData] = None)

  object DesignatoryDetails {
    implicit val hnformat = Json.format[HodName]
    implicit val dddformat = Json.format[DesignatoryDetailsData]

    implicit val readDesignatoryDetailsFormat = Json.reads[DesignatoryDetails]
    implicit val writeDesignatoryDetailsFormat = Json.writes[DesignatoryDetails]
  }

  def employerDetails(empref: String, authToken: String)(implicit rh: RequestHeader): Future[Either[String, EmployerDetail]] = {
    val uri = config.api.baseURI + s"/epaye/${helper.urlEncode(empref)}"

    ws.url(uri).withHeaders("Authorization" -> s"Bearer $authToken", "Accept" -> "application/vnd.hmrc.1.0+json").get.map { response =>
      response.status match {
        case 200 =>
          response.json.validate[DesignatoryDetails].fold(
            { (errs: Seq[(JsPath, Seq[ValidationError])]) =>
              formatErrs(errs).foreach(Logger.warn(_))
              Left(s"failed to unmarshall json")
            },
            details => {
              val name = details.employer.flatMap(_.name)
              Right(EmployerDetail(empref, name.flatMap(_.nameLine1), name.flatMap(_.nameLine2)))
            }
          )

        case _ =>
          Logger.error(s"Got status ${response.status} calling levy api")
          Logger.error(uri)
          Logger.error(response.body)
          Right(EmployerDetail(empref, None, None))
      }
    }
  }

  def formatErrs(errs: Seq[(JsPath, Seq[ValidationError])]): Seq[String] = {
    errs.flatMap {
      case (path, es) => es.map { e =>
        s"$path: ${e.message}"
      }
    }
  }
}
