package controllers

import javax.inject.Inject

import db.SchemeClaimOps
import play.api.mvc.{Action, Controller}

import scala.concurrent.ExecutionContext

class AdminController @Inject()(claims: SchemeClaimOps)(implicit ec: ExecutionContext) extends Controller {

  def index() = Action.async { implicit request =>
    claims.all().map(cs => Ok(views.html.adminIndex(cs.sortBy(_.empref))))
  }

  def expireToken(token: String) = Action.async { implicit request =>
    claims.expireToken(token).map(_ => Redirect(controllers.routes.AdminController.index()))
  }

}
