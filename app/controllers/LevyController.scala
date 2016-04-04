package controllers

import javax.inject.Inject

import cats.data.Xor._
import db.{SchemeClaimDAO, SchemeClaimRow}
import play.api.Logger
import play.api.mvc._
import services.OAuth2Service

import scala.concurrent.{ExecutionContext, Future}

class LevyController @Inject()(levyApi: LevyApi, config: ServiceConfig, oAuth2Service: OAuth2Service, schemeClaims: SchemeClaimDAO)(implicit ec: ExecutionContext) extends Controller {

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
      for {
        updatedRow <- oAuth2Service.refreshAccessToken(row)
        _ <- schemeClaims.updateClaim(updatedRow)
      } yield updatedRow.accessToken
    } else {
      Future.successful(row.accessToken)
    }
  }


}
