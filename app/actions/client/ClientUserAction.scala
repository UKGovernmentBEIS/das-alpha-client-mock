package actions.client

import com.google.inject.Inject
import db.client.{DASUserDAO, DASUserRow}
import play.api.mvc.Results._
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try


class ClientUserRequest[A](val request: Request[A], val user: DASUserRow) extends WrappedRequest[A](request)

class ClientUserAction @Inject()(dasUsers: DASUserDAO)(implicit ec: ExecutionContext)
  extends ActionBuilder[ClientUserRequest]
    with ActionRefiner[Request, ClientUserRequest] {

  val sessionKey = "userId"

  override protected def refine[A](request: Request[A]): Future[Either[Result, ClientUserRequest[A]]] = {

    val login = Left(Redirect(controllers.client.routes.ClientLoginController.showLogin()))
    request.session.get(sessionKey) match {
      case None => Future.successful(login)
      case Some(ParseLong(id)) => dasUsers.byId(id).map {
        case Some(u) => Right(new ClientUserRequest(request, u))
        case None => login
      }
      case Some(s) => Future.successful(login)
    }
  }
}

object ParseLong {
  def unapply(s: String): Option[Long] = Try(s.toLong).toOption
}
