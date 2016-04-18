package data

import com.google.inject.ImplementedBy
import db.DASUserDAO

import scala.concurrent.Future

case class DASUser(id: Long, name: String, hashedPassword: String)

@ImplementedBy(classOf[DASUserDAO])
trait DASUserOps {
  def byId(id: Long): Future[Option[DASUser]]

  def byName(s: String): Future[Option[DASUser]]

  def validate(username: String, password: String): Future[Option[DASUser]]
}