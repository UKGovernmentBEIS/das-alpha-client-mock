package db

import javax.inject.{Inject, Singleton}

import data.{DASUser, DASUserOps, UserId}
import play.api.db.slick.DatabaseConfigProvider

import scala.concurrent.{ExecutionContext, Future}

trait DASUserModule extends SlickModule {

  import driver.api._

  implicit def UserIdMapper: BaseColumnType[UserId] = MappedColumnType.base[UserId, Long](_.id, UserId)

  val dasUsers = TableQuery[DASUserTable]

  class DASUserTable(tag: Tag) extends Table[DASUser](tag, "das_user") {

    def id = column[UserId]("id", O.PrimaryKey)

    def name = column[String]("name")

    def password = column[String]("password")

    def * = (id, name, password) <> (DASUser.tupled, DASUser.unapply)
  }

}

@Singleton
class DASUserDAO @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)(implicit val ec: ExecutionContext) extends DASUserModule with DASUserOps {

  import driver.api._

  override def validate(username: String, password: String): Future[Option[DASUser]] = db.run {
    dasUsers.filter(u => u.name === username && u.password === password).result.headOption
  }

  override def byId(id: UserId): Future[Option[DASUser]] = db.run(dasUsers.filter(_.id === id).result.headOption)

  override def byName(s: String): Future[Option[DASUser]] = db.run(dasUsers.filter(u => u.name === s).result.headOption)
}