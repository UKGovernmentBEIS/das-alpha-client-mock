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
          scr <- convertCode(c, request.user.id, empref)
          _ <- schemeClaimDAO.insert(scr)
        } yield redirectToIndex
      }
    }.getOrElse(Future.successful(BadRequest("no 'empref' was present in session")))
  }

  case class AccessTokenResponse(access_token: String, expires_in: Long, scope: String, refresh_token: String, token_type: String)

  implicit val atrFormat = Json.format[AccessTokenResponse]

  def convertCode(code: String, userId: Long, empref: String)(implicit requestHeader: RequestHeader): Future[SchemeClaimRow] = {
    val params = Map(
      "grant_type" -> "authorization_code",
      "code" -> code,
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

