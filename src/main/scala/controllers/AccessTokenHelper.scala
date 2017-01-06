package controllers

import javax.inject.Inject

import data.{AccessToken, SchemeClaimOps, SchemeClaimRow}
import play.api.Logger
import play.api.mvc.RequestHeader
import services.OAuth2Service

import scala.concurrent.{ExecutionContext, Future}

class AccessTokenHelper @Inject()(oAuth2Service: OAuth2Service, claims: SchemeClaimOps)(implicit ec: ExecutionContext) {
  def freshenAccessToken(row: SchemeClaimRow)(implicit requestHeader: RequestHeader): Future[Option[AccessToken]] = {
    if (row.isAuthTokenExpired) {
      Logger.info(s"access token has expired - refreshing")
      oAuth2Service.refreshAccessToken(row.refreshToken).flatMap {
        case Some(rtr) =>
          val validUntil = System.currentTimeMillis() + (rtr.expires_in * 1000)
          val updatedRow = row.copy(accessToken = rtr.access_token, validUntil = validUntil)
          claims.updateClaim(updatedRow).map(_ => Some(updatedRow.accessToken))

        case None =>
          Logger.warn(s"Failed to refresh access token using refresh token ${row.refreshToken}")
          Future.successful(None)
      }
    } else {
      Future.successful(Some(row.accessToken))
    }
  }

  def freshenPrivilegedAccessToken(implicit requestHeader: RequestHeader): Future[Option[AccessToken]] = {
    oAuth2Service.refreshPrivilegedAccessToken.flatMap {
      case Some(rtr) =>
        Logger.debug(s"access token is ${rtr.access_token}")
        Future.successful(Some(rtr.access_token))

      case None =>
        Logger.warn(s"Failed to refresh privileged access token")
        Future.successful(None)
    }
  }
}
