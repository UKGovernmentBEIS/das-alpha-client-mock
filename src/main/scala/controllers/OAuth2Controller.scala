package controllers

import javax.inject.Inject

import actions.ClientUserAction
import data.{SchemeClaim, SchemeClaimOps}
import play.api.mvc._
import services.OAuth2Service
import services.ServiceConfig.config

import scala.concurrent.{ExecutionContext, Future}
class OAuth2Controller @Inject()(oAuth2Service: OAuth2Service, claims: SchemeClaimOps, userAction: ClientUserAction)(implicit exec: ExecutionContext) extends Controller {

  def startOauthDance(empref: String)(implicit request: RequestHeader): Future[Result] = {
    val params = Map(
      "client_id" -> Seq(config.client.id),
      "redirect_uri" -> Seq(routes.OAuth2Controller.claimCallback(None, None).absoluteURL(config.client.useSSL)),
      "scope" -> Seq("read:apprenticeship-levy"),
      "response_type" -> Seq("code")
    )
    Future.successful(Redirect(config.taxservice.authorizeSchemeUri, params).addingToSession("empref" -> empref))
  }

  def claimCallback(code: Option[String], state: Option[String]) = userAction.async { implicit request =>
    val redirectToIndex = Redirect(controllers.routes.ClientController.index())

    request.session.get("empref").map { empref =>
      code match {
        case None => Future.successful(redirectToIndex)
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

