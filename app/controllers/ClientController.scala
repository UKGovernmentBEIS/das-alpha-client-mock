package controllers

import javax.inject.{Inject, Singleton}

import actions.{ClientUserAction, ClientUserRequest}
import db.{DASUserDAO, SchemeClaimDAO, SchemeClaimRow}
import play.api.Logger
import play.api.data.Form
import play.api.data.Forms._
import play.api.data.validation._
import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ClientController @Inject()(config: ServiceConfig, ws: WSClient, dasUserDAO: DASUserDAO, UserAction: ClientUserAction, schemeClaimDAO: SchemeClaimDAO)(implicit exec: ExecutionContext) extends Controller {

  import config._

  def unclaimed(claimed: Seq[SchemeClaimRow]): Constraint[String] = Constraint[String]("already claimed") { empref =>
    if (claimed.map(_.empref.trim()).contains(empref.trim())) Invalid(ValidationError(s"you have already claimed scheme $empref"))
    else Valid
  }

  def claimMapping(claimedSchemes: Seq[SchemeClaimRow]) = Form("empref" -> text.verifying(unclaimed(claimedSchemes)))

  def index = Action {
    Redirect(controllers.routes.ClientController.showClaimScheme())
  }

  def showClaimScheme = UserAction.async { request =>
    showClaimPage(request)
  }

  def showClaimPage(request: ClientUserRequest[AnyContent]): Future[Result] = {
    schemeClaimDAO.forUser(request.user.id).map { claimedSchemes =>
      Ok(views.html.claimScheme(claimMapping(claimedSchemes), request.user, claimedSchemes))
    }
  }

  def claimScheme = UserAction.async { implicit request =>
    schemeClaimDAO.forUser(request.user.id).flatMap { claimedSchemes =>
      claimMapping(claimedSchemes).bindFromRequest().fold(
        formWithErrors => Future.successful(Ok(views.html.claimScheme(formWithErrors, request.user, claimedSchemes))),
        empref => oathDance(empref)
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

  def oathDance(empref: String)(implicit request: RequestHeader): Future[Result] = {
    val params = Map(
      "client_id" -> Seq(clientId),
      "redirect_uri" -> Seq(routes.ClientController.claimCallback(None, None).absoluteURL(request.secure)),
      "scope" -> Seq("read:apprenticeship-levy")
    )
    Future.successful(Redirect(authorizeSchemeUri, params).addingToSession("empref" -> empref))
  }

  def claimCallback(code: Option[String], state: Option[String]) = UserAction.async { implicit request =>
    val redirectToIndex = Redirect(controllers.routes.ClientController.index())

    request.session.get("empref").map { empref =>
      code match {
        case None => Future.successful(Redirect(controllers.routes.ClientController.index()))
        case Some(c) => convertCode(c, request.user.id, empref).flatMap {
          case Some(scr) => schemeClaimDAO.insert(scr).map { _ => redirectToIndex }
          case None => Future.successful(redirectToIndex)
        }
      }
    }.getOrElse(Future.successful(BadRequest("no 'empref' was present in session")))
  }

  case class AccessTokenResponse(access_token: String, expires_in: Long, scope: String, refresh_token: String, token_type: String)

  object AccessTokenResponse {
    implicit val format = Json.format[AccessTokenResponse]
  }

  def convertCode(code: String, userId: Long, empref: String)(implicit requestHeader: RequestHeader): Future[Option[SchemeClaimRow]] = {
    callAuthServer(userId, code, empref).map(Some(_))
  }


  def callAuthServer(userId: Long, authCode: String, empref: String)(implicit rh: RequestHeader): Future[SchemeClaimRow] = {
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
          Logger.info(Json.prettyPrint(response.json))
          val validUntil = System.currentTimeMillis() + (r.expires_in * 1000)
          SchemeClaimRow(empref, userId, r.access_token, validUntil, r.refresh_token)

        case 401 =>
          Logger.warn("Request to exchange code for token failed")
          Logger.warn(s"Response message is: '${response.body}'")
          throw new Exception(s"Request to exchange code for token failed with ${response.body}")
      }
    }
  }
}

