package controllers

import javax.inject.Inject

import cats.data.Xor._
import db.{SchemeClaimOps, SchemeClaimRow}
import play.api.Logger
import play.api.mvc._
import services.{LevyApiService, OAuth2Service, ServiceConfig}

import scala.concurrent.{ExecutionContext, Future}

class LevyController @Inject()(levyApi: LevyApiService, config: ServiceConfig, oAuth2Service: OAuth2Service, claims: SchemeClaimOps)(implicit ec: ExecutionContext) extends Controller {

  def showEmpref(empref: String) = Action.async { implicit request =>
    claims.forEmpref(empref).flatMap {
      case Some(row) =>
        withFreshAccessToken(row).flatMap {
          case Some(authToken) =>
            levyApi.declarations(empref, authToken).map {
              case Right(decls) => Ok(views.html.declarations(decls))
              case Left(err) => InternalServerError(err)
            }
          case None => Future.successful(BadRequest("Unable to refresh access token"))
        }

      case None => Future.successful(NotFound)
    }
  }

  def withFreshAccessToken(row: SchemeClaimRow)(implicit requestHeader: RequestHeader): Future[Option[String]] = {
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


}
