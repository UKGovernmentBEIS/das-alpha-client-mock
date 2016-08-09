package controllers

import javax.inject.Inject

import actions.{ClientUserAction, ClientUserRequest}
import cats.data.Xor
import cats.syntax.xor._
import cats.data.Xor.{Left, Right}
import data.{SchemeClaim, SchemeClaimOps}
import play.api.mvc._
import services.ServiceConfig.config
import services.{LevyApiService, OAuth2Service}

import scala.concurrent.{ExecutionContext, Future}

class OAuth2Controller @Inject()(oAuth2Service: OAuth2Service, api: LevyApiService, claims: SchemeClaimOps, userAction: ClientUserAction)(implicit exec: ExecutionContext) extends Controller {

  def startOauthDance(empref: String)(implicit request: RequestHeader): Future[Result] = {
    val params = Map(
      "client_id" -> Seq(config.client.id),
      "redirect_uri" -> Seq(routes.OAuth2Controller.claimCallback(None, None).absoluteURL(config.client.useSSL)),
      "scope" -> Seq("read:apprenticeship-levy"),
      "response_type" -> Seq("code")
    )
    Future.successful(Redirect(config.taxservice.authorizeSchemeUri, params).addingToSession("empref" -> empref))
  }

  case class AccessTokenDetails(accessToken: String, validUntil: Long, refreshToken: String)

  def claimCallback(code: Option[String], state: Option[String]) = userAction.async { implicit request =>
    val redirectToIndex = Redirect(controllers.routes.ClientController.index())

    val atd: Xor[Future[Result], Future[AccessTokenDetails]] = code match {
      case None => Left(Future.successful(BadRequest("No oAuth code")))
      case Some(c) => convertCodeToToken(request, c).right
    }

    atd.map { fd =>
      for {
        d <- fd
        response <- api.root(d.accessToken)
      } yield response match {
        case Left(err) => BadRequest(err)
        case Right(emprefs) => Ok(views.html.selectEmpref(request.user, emprefs.emprefs))
      }
    }.merge
  }

  def convertCodeToToken(request: ClientUserRequest[_], c: String): Future[AccessTokenDetails] = for {
    atr <- oAuth2Service.convertCode(c, request.user.id)
    validUntil = System.currentTimeMillis() + (atr.expires_in * 1000)
  } yield AccessTokenDetails(atr.access_token, validUntil, atr.refresh_token)
}

