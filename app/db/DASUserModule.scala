package db

import javax.inject.{Inject, Singleton}

import play.api.db.slick.DatabaseConfigProvider

import scala.concurrent.{ExecutionContext, Future}

case class DASUserRow(id: Long, name: String, hashedPassword: String)

trait DASUserModule extends DBModule {

  import driver.api._

  val DASUsers = TableQuery[DASUserTable]

  def validate(username: String, password: String): Future[Option[DASUserRow]] = db.run {
    DASUsers.filter(u => u.name === username && u.password === password).result.headOption
  }

  def byId(id: Long): Future[Option[DASUserRow]] = db.run(DASUsers.filter(_.id === id).result.headOption)

  def byName(s: String): Future[Option[DASUserRow]] = db.run(DASUsers.filter(u => u.name === s).result.headOption)

  class DASUserTable(tag: Tag) extends Table[DASUserRow](tag, "das_user") {

    def id = column[Long]("id", O.PrimaryKey)

    def name = column[String]("name")

    def password = column[String]("password")

    def * = (id, name, password) <>(DASUserRow.tupled, DASUserRow.unapply)
  }

}

@Singleton
class DASUserDAO @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)(implicit val ec: ExecutionContext) extends DASUserModule