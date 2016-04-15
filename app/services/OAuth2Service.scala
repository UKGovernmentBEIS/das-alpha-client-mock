package services

import javax.inject.Inject

import com.google.inject.ImplementedBy
import play.api.Logger
import play.api.libs.json.Json
import play.api.libs.ws.WSClient

import scala.concurrent.{ExecutionContext, Future}

case class AccessTokenResponse(access_token: String, expires_in: Long, scope: String, refresh_token: String, token_type: String)

case class RefreshTokenResponse(access_token: String, expires_in: Long)

@ImplementedBy(classOf[OAuth2ServiceImpl])
trait OAuth2Service {
  def convertCode(code: String, userId: Long, empref: String): Future[AccessTokenResponse]

  def refreshAccessToken(refreshToken: String): Future[Option[RefreshTokenResponse]]
}

class OAuth2ServiceImpl @Inject()(config: ServiceConfig, ws: WSClient)(implicit ec: ExecutionContext) extends OAuth2Service {

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

        case s =>
          Logger.warn("Request to exchange code for token failed")
          Logger.warn(s"Response is $s with body: '${response.body}'")
          throw new Exception(s"Request to exchange code for token failed with ${response.body}")

      }
    }
  }

  implicit val rtrFormat = Json.format[RefreshTokenResponse]

  def refreshAccessToken(refreshToken: String): Future[Option[RefreshTokenResponse]] = {
    val params = Map(
      "grant_type" -> "refresh_token",
      "refresh_token" -> refreshToken,
      "client_id" -> clientId,
      "client_secret" -> clientSecret
    ).map { case (k, v) => k -> Seq(v) }

    ws.url(accessTokenUri).post(params).map { response =>
      response.status match {
        case 200 => response.json.validate[RefreshTokenResponse].asOpt

        case s =>
          Logger.error(response.body)
          None
      }
    }
  }
}
