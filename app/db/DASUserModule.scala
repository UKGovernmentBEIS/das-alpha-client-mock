package db

import javax.inject.{Inject, Singleton}

import com.google.inject.ImplementedBy
import play.api.db.slick.DatabaseConfigProvider

import scala.concurrent.{ExecutionContext, Future}

case class DASUserRow(id: Long, name: String, hashedPassword: String)

@ImplementedBy(classOf[DASUserDAO])
trait DASUserOps {
  def byId(id: Long): Future[Option[DASUserRow]]

  def byName(s: String): Future[Option[DASUserRow]]

  def validate(username: String, password: String): Future[Option[DASUserRow]]
}

trait DASUserModule extends SlickModule {

  import driver.api._

  val DASUsers = TableQuery[DASUserTable]


  class DASUserTable(tag: Tag) extends Table[DASUserRow](tag, "das_user") {

    def id = column[Long]("id", O.PrimaryKey)

    def name = column[String]("name")

    def password = column[String]("password")

    def * = (id, name, password) <>(DASUserRow.tupled, DASUserRow.unapply)
  }

}

@Singleton
class DASUserDAO @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)(implicit val ec: ExecutionContext) extends DASUserModule with DASUserOps {

  import driver.api._

  override def validate(username: String, password: String): Future[Option[DASUserRow]] = db.run {
    DASUsers.filter(u => u.name === username && u.password === password).result.headOption
  }

  override def byId(id: Long): Future[Option[DASUserRow]] = db.run(DASUsers.filter(_.id === id).result.headOption)

  override def byName(s: String): Future[Option[DASUserRow]] = db.run(DASUsers.filter(u => u.name === s).result.headOption)
}