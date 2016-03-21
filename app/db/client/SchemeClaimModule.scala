package db.client

import java.sql.Date
import javax.inject.{Inject, Singleton}

import db.DBModule
import play.api.db.slick.DatabaseConfigProvider

import scala.concurrent.{ExecutionContext, Future}

case class SchemeClaimRow(empref: String, userId: Long, authToken: String, validUntil: Date, refreshToken: Option[String])

trait SchemeClaimModule extends DBModule {

  import driver.api._

  val SchemeClaims = TableQuery[SchemeClaimTable]

  def forUser(userId: Long): Future[Seq[SchemeClaimRow]] = db.run(SchemeClaims.filter(_.dasUserId === userId).result)

  def insert(cat: SchemeClaimRow): Future[Unit] = db.run(SchemeClaims += cat).map { _ => () }

  class SchemeClaimTable(tag: Tag) extends Table[SchemeClaimRow](tag, "SCHEME_CLAIM") {

    def empref = column[String]("EMPREF", O.PrimaryKey)

    def dasUserId = column[Long]("DAS_USER_ID")

    def accessToken = column[String]("ACCESS_TOKEN")

    def validUntil = column[Date]("VALID_UNTIL")

    def refreshToken = column[Option[String]]("REFRESH_TOKEN")


    def * = (empref, dasUserId, accessToken, validUntil, refreshToken) <>(SchemeClaimRow.tupled, SchemeClaimRow.unapply)
  }

}

@Singleton
class SchemeClaimDAO @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)(implicit val ec: ExecutionContext) extends SchemeClaimModule