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
class ClientSignInController @Inject()(dasUserDAO: DASUserDAO, UserAction: ClientUserAction)(implicit exec: ExecutionContext) extends Controller {

  val signInForm = Form(
    mapping(
      "username" -> text,
      "password" -> text
    )(ClientUserData.apply)(ClientUserData.unapply)
  )

  def showSignIn = Action {
    Ok(views.html.login(signInForm))
  }

  def handleSignIn = Action.async { implicit request =>
    signInForm.bindFromRequest.fold(
      formWithErrors => Future.successful(BadRequest(views.html.login(formWithErrors))),
      userData => {
        dasUserDAO.validate(userData.name, userData.password).map {
          case Some(user) => Redirect(controllers.routes.ClientController.index()).addingToSession(UserAction.sessionKey -> user.id.toString)
          case None => Ok(views.html.login(signInForm.withError("username", "Bad user name or password")))
        }
      }
    )
  }

  def signOut = Action {
    Redirect(controllers.routes.ClientSignInController.showSignIn()).withNewSession
  }
}
