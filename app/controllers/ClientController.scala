package controllers

import javax.inject.{Inject, Singleton}

import actions.ClientUserAction
import data.{DASUserOps, SchemeClaim, SchemeClaimOps}
import play.api.data.Form
import play.api.data.Forms._
import play.api.data.validation._
import play.api.mvc._
import services.{OAuth2Service, ServiceConfig}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ClientController @Inject()(oAuth2Service: OAuth2Service, users: DASUserOps, claims: SchemeClaimOps, UserAction: ClientUserAction)(implicit exec: ExecutionContext) extends Controller {

  import ServiceConfig.config._

  def unclaimed(claims: Seq[SchemeClaim], userId: Long): Constraint[String] = Constraint[String]("already claimed") { empref =>
    if (claims.exists(row => row.empref.trim() == empref.trim() && row.userId == userId)) Invalid(ValidationError(s"you have already claimed scheme $empref"))
    else if (claims.exists(row => row.empref.trim() == empref.trim())) Invalid(ValidationError(s"another user has already claimed scheme $empref"))
    else Valid
  }

  def claimMapping(claimedSchemes: Seq[SchemeClaim], userId: Long) = Form("empref" -> nonEmptyText.verifying(unclaimed(claimedSchemes, userId)))

  def index = Action {
    Redirect(controllers.routes.ClientController.showClaimScheme())
  }

  def showClaimScheme = UserAction.async { request =>
    claims.forUser(request.user.id).map { claimedSchemes =>
      Ok(views.html.claimScheme(claimMapping(claimedSchemes, request.user.id), request.user, claimedSchemes))
    }
  }

  def claimScheme = UserAction.async { implicit request =>
    claims.all().flatMap { allClaims =>
      claimMapping(allClaims, request.user.id).bindFromRequest().fold(
        formWithErrors => Future.successful(Ok(views.html.claimScheme(formWithErrors, request.user, allClaims.filter(_.userId == request.user.id)))),
        empref => startOauthDance(empref)
      )
    }
  }

  def removeScheme(empref: String) = UserAction.async { implicit request =>
    claims.removeClaimForUser(empref, request.user.id).map { count =>
      Redirect(controllers.routes.ClientController.index())
    }
  }

  def startOauthDance(empref: String)(implicit request: RequestHeader): Future[Result] = {
    val params = Map(
      "client_id" -> Seq(client.id),
      "redirect_uri" -> Seq(routes.ClientController.claimCallback(None, None).absoluteURL(client.useSSL)),
      "scope" -> Seq("read:apprenticeship-levy"),
      "response_type" -> Seq("code")
    )
    Future.successful(Redirect(taxservice.authorizeSchemeUri, params).addingToSession("empref" -> empref))
  }

  def claimCallback(code: Option[String], state: Option[String]) = UserAction.async { implicit request =>
    val redirectToIndex = Redirect(controllers.routes.ClientController.index())

    request.session.get("empref").map { empref =>
      code match {
        case None => Future.successful(Redirect(controllers.routes.ClientController.index()))
        case Some(c) => for {
          atr <- oAuth2Service.convertCode(c, request.user.id, empref)
          validUntil = System.currentTimeMillis() + (atr.expires_in * 1000)
          scr = SchemeClaim(empref, request.user.id, atr.access_token, validUntil, atr.refresh_token)
          _ <- claims.insert(scr)
        } yield redirectToIndex
      }
    }.getOrElse(Future.successful(BadRequest("no 'empref' was present in session")))
  }

}

