package controllers

import javax.inject.Inject

import db.{SchemeClaimDAO, SchemeClaimRow}
import models.LevyDeclarations
import play.api.Logger
import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import play.api.mvc.{Action, Controller, RequestHeader}
import views.html.helper

import scala.concurrent.{ExecutionContext, Future}

class LevyController @Inject()(config: ServiceConfig, ws: WSClient, schemeClaims: SchemeClaimDAO)(implicit ec: ExecutionContext) extends Controller {

  import config._

  def showEmpref(empref: String) = Action.async { implicit request =>
    schemeClaims.forEmpref(empref).flatMap {
      case Some(row) =>
        val uri = apiBaseURI + s"/${helper.urlEncode(empref)}/levy-declarations"
        withFreshAccessToken(row).flatMap { authToken =>
          Logger.info(s"calling $uri")
          ws.url(uri).withHeaders("Authorization" -> s"Bearer $authToken").get.map { response =>
            response.status match {
              case 200 =>
                Logger.info(response.body)
                val decls = response.json.validate[LevyDeclarations]
                Ok(views.html.declarations(decls.get))

              case _ =>
                Logger.error(s"Got status ${response.status}")
                Logger.error(response.body)
                throw new Exception(s"Got status ${response.status}")
            }
          }
        }

      case None => Future.successful(NotFound)
    }
  }

  def withFreshAccessToken(row: SchemeClaimRow)(implicit requestHeader: RequestHeader): Future[String] = {
    if (row.isAuthTokenExpired) {
      Logger.info(s"access token has expired - refreshing")
      refreshAccessToken(row).map(_.accessToken)
    } else {
      Future.successful(row.accessToken)
    }
  }

  case class RefreshTokenResponse(access_token: String, expires_in: Long)

  object RefreshTokenResponse {
    implicit val format = Json.format[RefreshTokenResponse]
  }

  def refreshAccessToken(row: SchemeClaimRow)(implicit rh: RequestHeader): Future[SchemeClaimRow] = {
    val params = Map(
      "grant_type" -> "refresh_token",
      "refresh_token" -> row.refreshToken,
      "client_id" -> clientId,
      "client_secret" -> clientSecret
    ).map { case (k, v) => k -> Seq(v) }

    ws.url(accessTokenUri).post(params).map { response =>
      response.status match {
        case 200 =>
          val r = response.json.validate[RefreshTokenResponse].get
          Logger.info(Json.prettyPrint(response.json))
          val validUntil = System.currentTimeMillis() + (r.expires_in * 1000)
          row.copy(accessToken = r.access_token, validUntil = validUntil)

        case 401 =>
          Logger.error(response.body)
          throw new Exception(response.body)

        case 400 =>
          Logger.error(response.body)
          throw new Exception("bad request")
      }
    }
  }

}
