package services

import javax.inject.Inject

import controllers.ServiceConfig
import db.SchemeClaimRow
import play.api.Logger
import play.api.libs.json.Json
import play.api.libs.ws.WSClient

import scala.concurrent.{ExecutionContext, Future}

class OAuth2Service @Inject()(config: ServiceConfig, ws: WSClient)(implicit ec: ExecutionContext) {

  import config._

  case class AccessTokenResponse(access_token: String, expires_in: Long, scope: String, refresh_token: String, token_type: String)

  implicit val atrFormat = Json.format[AccessTokenResponse]

  def convertCode(code: String, userId: Long, empref: String): Future[SchemeClaimRow] = {
    val params = Map(
      "grant_type" -> "authorization_code",
      "code" -> code,
      "redirect_uri" -> "http://localhost:9000/",
      "client_id" -> clientId,
      "client_secret" -> clientSecret
    ).map { case (k, v) => k -> Seq(v) }

    ws.url(accessTokenUri).post(params).map { response =>
      response.status match {
        case 200 =>
          val r = response.json.validate[AccessTokenResponse].get
          Logger.info(Json.prettyPrint(response.json))
          val validUntil = System.currentTimeMillis() + (r.expires_in * 1000)
          SchemeClaimRow(empref, userId, r.access_token, validUntil, r.refresh_token)

        case 401 =>
          Logger.warn("Request to exchange code for token failed")
          Logger.warn(s"Response message is: '${response.body}'")
          throw new Exception(s"Request to exchange code for token failed with ${response.body}")
      }
    }
  }

  case class RefreshTokenResponse(access_token: String, expires_in: Long)

  object RefreshTokenResponse {
    implicit val format = Json.format[RefreshTokenResponse]
  }

  def refreshAccessToken(row: SchemeClaimRow): Future[SchemeClaimRow] = {
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
