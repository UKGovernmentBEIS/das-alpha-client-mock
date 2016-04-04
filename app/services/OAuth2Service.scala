package services

import javax.inject.Inject

import controllers.ServiceConfig
import play.api.Logger
import play.api.libs.json.Json
import play.api.libs.ws.WSClient

import scala.concurrent.{ExecutionContext, Future}

case class AccessTokenResponse(access_token: String, expires_in: Long, scope: String, refresh_token: String, token_type: String)

case class RefreshTokenResponse(access_token: String, expires_in: Long)

class OAuth2Service @Inject()(config: ServiceConfig, ws: WSClient)(implicit ec: ExecutionContext) {

  import config._

  implicit val atrFormat = Json.format[AccessTokenResponse]

  def convertCode(code: String, userId: Long, empref: String): Future[AccessTokenResponse] = {
    val params = Map(
      "grant_type" -> "authorization_code",
      "code" -> code,
      "redirect_uri" -> "http://localhost:9000/",
      "client_id" -> clientId,
      "client_secret" -> clientSecret
    ).map { case (k, v) => k -> Seq(v) }

    ws.url(accessTokenUri).post(params).map { response =>
      response.status match {
        case 200 => response.json.validate[AccessTokenResponse].get

        case 401 =>
          Logger.warn("Request to exchange code for token failed")
          Logger.warn(s"Response message is: '${response.body}'")
          throw new Exception(s"Request to exchange code for token failed with ${response.body}")

      }
    }
  }

  implicit val rtrFormat = Json.format[RefreshTokenResponse]

  def refreshAccessToken(refreshToken:String): Future[RefreshTokenResponse] = {
    val params = Map(
      "grant_type" -> "refresh_token",
      "refresh_token" -> refreshToken,
      "client_id" -> clientId,
      "client_secret" -> clientSecret
    ).map { case (k, v) => k -> Seq(v) }

    ws.url(accessTokenUri).post(params).map { response =>
      response.status match {
        case 200 => response.json.validate[RefreshTokenResponse].get

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
