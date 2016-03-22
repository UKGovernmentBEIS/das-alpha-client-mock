package controllers.client

import javax.inject.Inject

import db.client.SchemeClaimDAO
import models.LevyDeclarations
import play.api.Logger
import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import play.api.mvc.{Action, Controller}
import views.html.helper

import scala.concurrent.{ExecutionContext, Future}

class LevyController @Inject()(ws: WSClient, schemeClaims: SchemeClaimDAO)(implicit ec: ExecutionContext) extends Controller {

  val apiUriBase = "http://localhost:9001/epaye"

  def showEmpref(empref: String) = Action.async { implicit request =>
    schemeClaims.forEmpref(empref).flatMap {
      case Some(row) =>

        val uri = apiUriBase + s"/${helper.urlEncode(empref)}/levy-declarations"
        Logger.info(s"calling $uri")

        ws.url(uri).withHeaders("Authorization" -> s"Bearer ${row.authToken}").get.map { response =>
          Logger.info(response.body)
          val decls = response.json.validate[LevyDeclarations]
          Ok(views.html.client.declarations(decls.get))
        }

      case None => Future.successful(NotFound)
    }
  }

}
