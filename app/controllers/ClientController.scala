package controllers

import javax.inject.{Inject, Singleton}

import actions.ClientUserAction
import db.{DASUserDAO, SchemeClaimDAO, SchemeClaimRow}
import play.api.Logger
import play.api.data.Form
import play.api.data.Forms._
import play.api.data.validation._
import play.api.mvc._
import services.OAuth2Service

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ClientController @Inject()(config: ServiceConfig, oAuth2Service: OAuth2Service, dasUserDAO: DASUserDAO, UserAction: ClientUserAction, schemeClaimDAO: SchemeClaimDAO)(implicit exec: ExecutionContext) extends Controller {

  import config._

  def unclaimed(claims: Seq[SchemeClaimRow], userId: Long): Constraint[String] = Constraint[String]("already claimed") { empref =>
    if (claims.exists(row => row.empref.trim() == empref.trim() && row.userId == userId)) Invalid(ValidationError(s"you have already claimed scheme $empref"))
    else if (claims.exists(row => row.empref.trim() == empref.trim())) Invalid(ValidationError(s"another user has already claimed scheme $empref"))
    else Valid
  }

  def claimMapping(claimedSchemes: Seq[SchemeClaimRow], userId: Long) = Form("empref" -> nonEmptyText.verifying(unclaimed(claimedSchemes, userId)))

  def index = Action {
    Redirect(controllers.routes.ClientController.showClaimScheme())
  }

  def showClaimScheme = UserAction.async { request =>
    schemeClaimDAO.forUser(request.user.id).map { claimedSchemes =>
      Ok(views.html.claimScheme(claimMapping(claimedSchemes, request.user.id), request.user, claimedSchemes))
    }
  }

  def claimScheme = UserAction.async { implicit request =>
    schemeClaimDAO.all().flatMap { allClaims =>
      claimMapping(allClaims, request.user.id).bindFromRequest().fold(
        formWithErrors => Future.successful(Ok(views.html.claimScheme(formWithErrors, request.user, allClaims.filter(_.userId == request.user.id)))),
        empref => startOauthDance(empref)
      )
    }
  }

  def removeScheme(empref: String) = UserAction.async { implicit request =>
    Logger.info(s"Trying to remove claim of $empref for user ${request.user.id}")
    schemeClaimDAO.removeClaimForUser(empref, request.user.id).map { count =>
      Logger.info(s"removed $count rows")
      Redirect(controllers.routes.ClientController.index())
    }
  }

  def startOauthDance(empref: String)(implicit request: RequestHeader): Future[Result] = {
    val params = Map(
      "client_id" -> Seq(clientId),
      "redirect_uri" -> Seq(routes.ClientController.claimCallback(None, None).absoluteURL(useSSL)),
      "scope" -> Seq("read:apprenticeship-levy")
    )
    Future.successful(Redirect(authorizeSchemeUri, params).addingToSession("empref" -> empref))
  }

  def claimCallback(code: Option[String], state: Option[String]) = UserAction.async { implicit request =>
    val redirectToIndex = Redirect(controllers.routes.ClientController.index())

    request.session.get("empref").map { empref =>
      code match {
        case None => Future.successful(Redirect(controllers.routes.ClientController.index()))
        case Some(c) => for {
          scr <- oAuth2Service.convertCode(c, request.user.id, empref)
          _ <- schemeClaimDAO.insert(scr)
        } yield redirectToIndex
      }
    }.getOrElse(Future.successful(BadRequest("no 'empref' was present in session")))
  }

}

