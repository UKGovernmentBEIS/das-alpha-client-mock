package controllers

import javax.inject.{Inject, Singleton}

import actions.ClientUserAction
import db.DASUserDAO
import play.api.data.Form
import play.api.data.Forms._
import play.api.mvc.{Action, Controller}

import scala.concurrent.{ExecutionContext, Future}

case class ClientUserData(name: String, password: String)

@Singleton
class ClientLoginController @Inject()(dasUserDAO: DASUserDAO, UserAction: ClientUserAction)(implicit exec: ExecutionContext) extends Controller {

  val userForm = Form(
    mapping(
      "username" -> text,
      "password" -> text
    )(ClientUserData.apply)(ClientUserData.unapply)
  )

  def showLogin = Action {
    Ok(views.html.login(userForm))
  }

  def handleLogin = Action.async { implicit request =>
    userForm.bindFromRequest.fold(
      formWithErrors => Future.successful(BadRequest(views.html.login(formWithErrors))),
      userData => {
        dasUserDAO.validate(userData.name, userData.password).map {
          case Some(user) => Redirect(controllers.routes.ClientController.index()).addingToSession(UserAction.sessionKey -> user.id.toString)
          case None => Ok(views.html.login(userForm.withError("username", "Bad user name or password")))
        }
      }
    )
  }

  def logout = Action {
    Redirect(controllers.routes.ClientLoginController.showLogin()).withNewSession
  }
}
