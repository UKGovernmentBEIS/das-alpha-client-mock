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

class LevyController @Inject()(ws: WSClient, schemeClaims: SchemeClaimDAO)(implicit ec: ExecutionContext) extends Controller {

  val apiUriBase = "http://localhost:9001/epaye"

  def showEmpref(empref: String) = Action.async { implicit request =>
    schemeClaims.forEmpref(empref).flatMap {
      case Some(row) =>
        val uri = apiUriBase + s"/${helper.urlEncode(empref)}/levy-declarations"
        withFreshAuthToken(row).flatMap { authToken =>
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

  def withFreshAuthToken(row: SchemeClaimRow)(implicit requestHeader: RequestHeader): Future[String] = {
    if (row.isAuthTokenExpired) {
      Logger.info(s"access token has expired - refreshing")
      callAuthServer(row).map(_.accessToken)
    } else {
      Future.successful(row.accessToken)
    }
  }

  // TODO: Read from config or db
  val clientId = "client1"
  val clientSecret = "secret1"
  val accessTokenUri = "http://localhost:9002/oauth/token"

  case class RefreshTokenResponse(access_token: String, expires_in: Long)

  object RefreshTokenResponse {
    implicit val format = Json.format[RefreshTokenResponse]
  }

  def callAuthServer(row: SchemeClaimRow)(implicit rh: RequestHeader): Future[SchemeClaimRow] = {
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

        case 400 =>
          Logger.error(response.body)
          throw new Exception("bad request")
      }
    }
  }

}
