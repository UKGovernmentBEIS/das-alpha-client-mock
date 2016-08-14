package controllers

import javax.inject.Inject

import actions.ClientUserAction
import cats.data.Xor.{Left, Right}
import cats.data.{Xor, XorT}
import cats.std.future._
import cats.syntax.xor._
import data.{SchemeClaimOps, StashedTokenDetails, TokenStashOps}
import play.api.mvc._
import services.ServiceConfig.config
import services.{LevyApiService, OAuth2Service}

import scala.concurrent.{ExecutionContext, Future}


class OAuth2Controller @Inject()(oAuth2Service: OAuth2Service, accessTokens: TokenStashOps, schemes: SchemeClaimOps, api: LevyApiService, userAction: ClientUserAction)(implicit exec: ExecutionContext) extends Controller {

  def startOauthDance(implicit request: RequestHeader): Result = {
    val params = Map(
      "client_id" -> Seq(config.client.id),
      "redirect_uri" -> Seq(routes.OAuth2Controller.claimCallback(None, None).absoluteURL(config.client.useSSL)),
      "scope" -> Seq("read:apprenticeship-levy"),
      "response_type" -> Seq("code")
    )
    Redirect(config.taxservice.authorizeSchemeUri, params)
  }

  case class TokenDetails(accessToken: String, validUntil: Long, refreshToken: String)

  def claimCallback(code: Option[String], state: Option[String]) = userAction.async { implicit request =>
    val redirectToIndex = Redirect(controllers.routes.ClientController.index())

    val tokenDetails: Future[Xor[Result, TokenDetails]] = code match {
      case None => Future.successful(Left(BadRequest("No oAuth code")))
      case Some(c) => convertCodeToToken(c).map(_.right)
    }

    val refx = for {
      td <- XorT(tokenDetails)
      emprefs <- XorT(api.root(td.accessToken).map(r => r.leftMap(BadRequest(_))))
      ds = emprefs.emprefs.map(StashedTokenDetails(_, td.accessToken, td.validUntil, td.refreshToken, request.user.id))
      ref <- XorT[Future, Result, Int](accessTokens.stash(ds).map(_.right))
    } yield ref

    refx.value.map {
      case Left(result) => result
      case Right(ref) => Redirect(controllers.routes.ClientController.selectSchemes(ref))
    }
  }

  def convertCodeToToken(c: String): Future[TokenDetails] = for {
    atr <- oAuth2Service.convertCode(c)
    validUntil = System.currentTimeMillis() + (atr.expires_in * 1000)
  } yield TokenDetails(atr.access_token, validUntil, atr.refresh_token)
}

