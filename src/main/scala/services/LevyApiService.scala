package services

import javax.inject.Inject

import cats.data.Xor
import cats.data.Xor._
import com.google.inject.ImplementedBy
import models.LevyDeclarations
import play.api.Logger
import play.api.data.validation.ValidationError
import play.api.libs.json.JsPath
import play.api.libs.ws.WSClient
import play.api.mvc.RequestHeader
import views.html.helper

import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[LevyApiImpl])
trait LevyApiService {
  def declarations(empref: String, authToken: String)(implicit rh: RequestHeader): Future[Xor[String, LevyDeclarations]]
}

class LevyApiImpl @Inject()( ws: WSClient)(implicit ec: ExecutionContext) extends LevyApiService {
  import ServiceConfig.config

  def declarations(empref: String, authToken: String)(implicit rh: RequestHeader): Future[Xor[String, LevyDeclarations]] = {
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
          Logger.error(response.body)
          Left(s"Got status ${response.status} with body '${response.body}' when calling levy api")
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
