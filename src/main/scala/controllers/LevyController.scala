package controllers

import javax.inject.Inject

import data.SchemeClaimOps
import play.api.mvc._
import services.LevyApiService

import scala.concurrent.{ExecutionContext, Future}

class LevyController @Inject()(levyApi: LevyApiService, tokenHelper: AccessTokenHelper, claims: SchemeClaimOps)(implicit ec: ExecutionContext) extends Controller {

  def showEmpref(empref: String) = Action.async { implicit request =>
    claims.forEmpref(empref).flatMap {
      case Some(row) =>
        tokenHelper.freshenPrivilegedAccessToken.flatMap {
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
}

