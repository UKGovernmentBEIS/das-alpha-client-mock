package controllers.client

import java.sql.Date
import javax.inject.{Inject, Singleton}

import actions.client.ClientUserAction
import db.client.{DASUserDAO, SchemeClaimDAO, SchemeClaimRow, SchemeDAO}
import org.joda.time.DateTime
import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import play.api.mvc.{Controller, Request, RequestHeader, Result}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ClientController @Inject()(ws: WSClient, schemeDAO: SchemeDAO, dasUserDAO: DASUserDAO, UserAction: ClientUserAction, schemeClaimDAO: SchemeClaimDAO)(implicit exec: ExecutionContext) extends Controller {
  def index = UserAction.async { request =>
    schemeClaimDAO.forUser(request.user.id).map { claimedSchemes =>
      Ok(views.html.client.index(request.user, claimedSchemes))
    }
  }

  val claimMapping = Form("empref" -> text)

  // TODO: Read from config
  val authorizeSchemeUri = "http://localhost:9002/oauth/authorize"

  def claimScheme = UserAction.async { implicit request =>
    claimMapping.bindFromRequest().fold(
      formWithErrors => Future.successful(Redirect(controllers.client.routes.ClientController.index())),
      empref => oathDance(empref)
    )
  }

  def oathDance(empref: String)(implicit request:RequestHeader): Future[Result] = {
    val params = Map(
      "clientId" -> Seq(clientId),
      "redirectURI" -> Seq(controllers.client.routes.ClientController.claimCallback(None).absoluteURL()),
      "scope" -> Seq(empref) // TODO: Improve scope handling
    )
    Future.successful(Redirect(authorizeSchemeUri, params))
  }

  def claimCallback(code: Option[String]) = UserAction.async { implicit request =>
    val redirectToIndex = Redirect(controllers.client.routes.ClientController.index())

    code match {
      case None => Future.successful(Redirect(controllers.client.routes.ClientController.index()))
      case Some(c) => convertCode(c, request.user.id).flatMap {
        case Some(scr) => schemeClaimDAO.insert(scr).map { _ => redirectToIndex }
        case None => Future.successful(redirectToIndex)
      }
    }
  }

  case class AccessTokenResponse(access_token: String, expires_in: Long, scope: String, refreshToken: Option[String], token_type: String)

  object AccessTokenResponse {
    implicit val format = Json.format[AccessTokenResponse]
  }

  def convertCode(code: String, userId: Long)(implicit requestHeader: RequestHeader): Future[Option[SchemeClaimRow]] = {
    callAuthServer(userId, code).map(Some(_))
  }

  // TODO: Read from config or db
  val clientId = "client1"
  val clientSecret = "secret1"
  val accessTokenUri = "http://localhost:9002/oauth/token"

  def callAuthServer(userId: Long, authCode: String)(implicit rh: RequestHeader): Future[SchemeClaimRow] = {
    val params = Map(
      "grant_type" -> "authorization_code",
      "code" -> authCode,
      "redirect_uri" -> "http://localhost:9000/",
      "client_id" -> clientId,
      "client_secret" -> clientSecret
    ).map { case (k, v) => k -> Seq(v) }

    ws.url(accessTokenUri).post(params).map { response =>
      response.status match {
        case 200 =>
          val r = response.json.validate[AccessTokenResponse].get
          val validUntil = DateTime.now.plus(r.expires_in * 1000)
          SchemeClaimRow(r.scope, userId, r.access_token, new Date(validUntil.getMillis), r.refreshToken)
      }
    }
  }
}

