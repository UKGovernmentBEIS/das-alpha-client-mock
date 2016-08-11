package controllers

import javax.inject.Inject

import actions.ClientUserAction
import data.{SchemeClaim, SchemeClaimOps}
import models.Emprefs
import play.api.data.Form
import play.api.data.Forms._
import play.api.data.validation._
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}

class ClientController @Inject()(oauth2Controller: OAuth2Controller, claims: SchemeClaimOps, userAction: ClientUserAction)(implicit exec: ExecutionContext) extends Controller {

  private[controllers] def unclaimed(claims: Seq[SchemeClaim], userId: Long): Constraint[String] = Constraint[String]("already claimed") { empref =>
    def alreadyClaimed(claim: SchemeClaim): Boolean = claim.empref.trim() == empref.trim()

    def claimedByUser(claim: SchemeClaim): Boolean = alreadyClaimed(claim) && claim.userId == userId

    empref match {
      case _ if claims.exists(claimedByUser) => Invalid(ValidationError(s"you have already claimed scheme $empref"))
      case _ if claims.exists(alreadyClaimed) => Invalid(ValidationError(s"another user has already claimed scheme $empref"))
      case _ => Valid
    }
  }

  private[controllers] def claimMapping(claimedSchemes: Seq[SchemeClaim], userId: Long) =
    Form("empref" -> nonEmptyText.verifying(unclaimed(claimedSchemes, userId)))

  def index = Action(Redirect(controllers.routes.ClientController.showClaimScheme()))

  def showClaimScheme = userAction.async { request =>
    claims.forUser(request.user.id).map { claimedSchemes =>
      Ok(views.html.claimScheme(claimMapping(claimedSchemes, request.user.id), request.user, claimedSchemes))
    }
  }

  def claimScheme = userAction.async { implicit request =>
    claims.all().flatMap { allClaims =>
      claimMapping(allClaims, request.user.id).bindFromRequest().fold(
        formWithErrors => Future.successful(Ok(views.html.claimScheme(formWithErrors, request.user, allClaims.filter(_.userId == request.user.id)))),
        empref => oauth2Controller.startOauthDance(empref)
      )
    }
  }

  def selectSchemes(ref: Long) = userAction.async { implicit request =>
    ???
  }

  def checkStatuses(userId: Long, emprefs: Seq[String]): Future[Seq[SchemeStatus]] = Future.sequence {
    emprefs.map { empref =>
      claims.forEmpref(empref) map {
        case None => Unclaimed(empref)
        case Some(claim) if claim.userId == userId => UserClaimed(empref)
        case Some(claim) => OtherClaimed(empref)
      }
    }
  }

  def removeScheme(empref: String) = userAction.async { implicit request =>
    claims.removeClaimForUser(empref, request.user.id).map { count =>
      Redirect(controllers.routes.ClientController.index())
    }
  }
}

