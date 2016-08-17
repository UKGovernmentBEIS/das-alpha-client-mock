package controllers

import javax.inject.Inject

import actions.ClientUserAction
import cats.data.Xor.{Left, Right}
import data.{SchemeClaim, SchemeClaimOps, TokenStashOps}
import models.EmployerDetail
import play.api.data.Form
import play.api.data.Forms._
import play.api.data.validation._
import play.api.mvc._
import services.LevyApiService

import scala.concurrent.{ExecutionContext, Future}

class ClientController @Inject()(oauth2Controller: OAuth2Controller, claims: SchemeClaimOps, stash: TokenStashOps, userAction: ClientUserAction, levy: LevyApiService)(implicit exec: ExecutionContext) extends Controller {

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

  def index = Action(Redirect(controllers.routes.ClientController.showSchemes()))

  def showSchemes = userAction.async { request =>
    claims.forUser(request.user.id).map { claimedSchemes =>
      Ok(views.html.showSchemes(claimMapping(claimedSchemes, request.user.id), request.user, claimedSchemes))
    }
  }

  def claimScheme = userAction { implicit request => oauth2Controller.startOauthDance }

  def selectSchemes(ref: Int) = userAction.async { implicit request =>
    val validTokens = stash.peek(ref).map(_.filter(_.userId == request.user.id))
    val employerDetails = validTokens.flatMap(tokens => Future.sequence(tokens.map(token => findEmployerDetails(token.empref, token.accessToken))))
    val statusF = employerDetails.flatMap(details => checkStatuses(request.user.id, details))

    statusF.map {
      case statuses => Ok(views.html.selectSchemes(request.user, statuses, ref))
    }
  }

  def findEmployerDetails(empref: String, accessToken: String)(implicit rh: RequestHeader): Future[EmployerDetail] =
    levy.employerDetails(empref, accessToken) map {
      case Left(s) => throw new Error(s)
      case Right(d) => d
    }

  def checkStatuses(userId: Long, details: Seq[EmployerDetail]): Future[Seq[SchemeStatus]] = Future.sequence {
    details.map { detail =>
      claims.forEmpref(detail.empref) map {
        case None => Unclaimed(detail)
        case Some(claim) if claim.userId == userId => UserClaimed(detail)
        case Some(claim) => OtherClaimed(detail)
      }
    }
  }

  def removeScheme(empref: String) = userAction.async { implicit request =>
    claims.removeClaimForUser(empref, request.user.id).map { count =>
      Redirect(controllers.routes.ClientController.index())
    }
  }
}

