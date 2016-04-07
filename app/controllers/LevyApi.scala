package controllers

import javax.inject.Inject

import cats.data.Xor
import cats.data.Xor._
import com.google.inject.ImplementedBy
import models.LevyDeclarations
import play.api.Logger
import play.api.libs.ws.WSClient
import play.api.mvc.RequestHeader
import views.html.helper

import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[LevyApiImpl])
trait LevyApi {
  def declarations(empref: String, authToken: String)(implicit rh: RequestHeader): Future[Xor[String, LevyDeclarations]]
}

class LevyApiImpl @Inject()(config: ServiceConfig, ws: WSClient)(implicit ec: ExecutionContext) extends LevyApi {
  def declarations(empref: String, authToken: String)(implicit rh: RequestHeader): Future[Xor[String, LevyDeclarations]] = {
    val uri = config.apiBaseURI + s"/${helper.urlEncode(empref)}/declarations"

    ws.url(uri).withHeaders("Authorization" -> s"Bearer $authToken").get.map { response =>
      response.status match {
        case 200 => Right(response.json.validate[LevyDeclarations].get)

        case _ =>
          Logger.error(s"Got status ${response.status} calling levy api")
          Logger.error(response.body)
          Left(s"Got status ${response.status} with body '${response.body}' when calling levy api")
      }
    }
  }
}
