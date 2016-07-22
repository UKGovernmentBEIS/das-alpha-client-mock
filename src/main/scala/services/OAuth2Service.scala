package services

import javax.inject.Inject

import com.google.inject.ImplementedBy
import play.api.Logger
import play.api.libs.json.{JsError, JsSuccess, Json}
import play.api.libs.ws.WSClient

import scala.concurrent.{ExecutionContext, Future}

case class AccessTokenResponse(access_token: String, expires_in: Long, scope: String, refresh_token: String, token_type: String)

case class RefreshTokenResponse(access_token: String, expires_in: Long)

@ImplementedBy(classOf[OAuth2ServiceImpl])
trait OAuth2Service {
  def convertCode(code: String, userId: Long, empref: String): Future[AccessTokenResponse]

  def refreshAccessToken(refreshToken: String): Future[Option[RefreshTokenResponse]]
}

class OAuth2ServiceImpl @Inject()(ws: WSClient)(implicit ec: ExecutionContext) extends OAuth2Service {

  import ServiceConfig.config._

  implicit val atrFormat = Json.format[AccessTokenResponse]

  def convertCode(code: String, userId: Long, empref: String): Future[AccessTokenResponse] = {
    val params = Map(
      "grant_type" -> "authorization_code",
      "code" -> code,
      "redirect_uri" -> taxservice.callbackURL,
      "client_id" -> client.id,
      "client_secret" -> client.secret
    ).map { case (k, v) => k -> Seq(v) }

    ws.url(taxservice.accessTokenUri).post(params).map { response =>
      response.status match {
        case 200 => response.json.validate[AccessTokenResponse] match {
          case JsSuccess(resp, _) => resp
          case JsError(errs) => throw new Exception(s"could not decode token response: $errs")
        }

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
      "client_id" -> client.id,
      "client_secret" -> client.secret
    ).map { case (k, v) => k -> Seq(v) }

    ws.url(taxservice.accessTokenUri).post(params).map { response =>
      response.status match {
        case 200 => response.json.validate[RefreshTokenResponse].asOpt

        case s =>
          Logger.error(response.body)
          None
      }
    }
  }
}
