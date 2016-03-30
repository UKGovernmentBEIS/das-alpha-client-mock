package controllers

import javax.inject.Inject

import cats.data.Xor._
import db.{SchemeClaimDAO, SchemeClaimRow}
import play.api.Logger
import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}

class LevyController @Inject()(levyApi: LevyApi, config: ServiceConfig, ws: WSClient, schemeClaims: SchemeClaimDAO)(implicit ec: ExecutionContext) extends Controller {

  import config._

  def showEmpref(empref: String) = Action.async { implicit request =>
    schemeClaims.forEmpref(empref).flatMap {
      case Some(row) =>
        withFreshAccessToken(row).flatMap { authToken =>
          levyApi.declarations(empref, authToken).map {
            case Right(decls) => Ok(views.html.declarations(decls))
            case Left(err) => InternalServerError(err)
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

    ws.url(accessTokenUri).post(params).flatMap { response =>
      response.status match {
        case 200 =>
          val r = response.json.validate[RefreshTokenResponse].get
          Logger.info(Json.prettyPrint(response.json))
          val validUntil = System.currentTimeMillis() + (r.expires_in * 1000)
          val updatedRow = row.copy(accessToken = r.access_token, validUntil = validUntil)
          schemeClaims.updateClaim(updatedRow).map(_ => row)

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
