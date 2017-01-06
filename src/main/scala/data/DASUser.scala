package data

import com.google.inject.ImplementedBy
import db.DASUserDAO

import scala.concurrent.Future

case class UserId(id:Long)

case class DASUser(id: UserId, name: String, hashedPassword: String)

@ImplementedBy(classOf[DASUserDAO])
trait DASUserOps {
  def byId(id: UserId): Future[Option[DASUser]]

  def byName(s: String): Future[Option[DASUser]]

  def validate(username: String, password: String): Future[Option[DASUser]]
}