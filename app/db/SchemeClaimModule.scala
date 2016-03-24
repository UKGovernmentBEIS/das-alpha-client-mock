package db

import javax.inject.{Inject, Singleton}

import org.joda.time.DateTime
import play.api.db.slick.DatabaseConfigProvider

import scala.concurrent.{ExecutionContext, Future}

case class SchemeClaimRow(empref: String, userId: Long, accessToken: String, validUntil: Long, refreshToken: String) {
  def isAuthTokenExpired: Boolean = new DateTime(validUntil).isBeforeNow
}

trait SchemeClaimModule extends DBModule {

  import driver.api._

  val SchemeClaims = TableQuery[SchemeClaimTable]

  def forUser(userId: Long): Future[Seq[SchemeClaimRow]] = db.run(SchemeClaims.filter(_.dasUserId === userId).result)

  def forEmpref(empref: String): Future[Option[SchemeClaimRow]] = db.run(SchemeClaims.filter(_.empref === empref).result.headOption)

  def removeClaimForUser(empref: String, userId: Long): Future[Int] = db.run {
    SchemeClaims.filter(sc => sc.empref === empref && sc.dasUserId === userId).delete
  }

  def removeAllClaimsForUser(userId: Long): Future[Int] = db.run(SchemeClaims.filter(_.dasUserId === userId).delete)

  def insert(cat: SchemeClaimRow): Future[Unit] = db.run(SchemeClaims += cat).map { _ => () }

  class SchemeClaimTable(tag: Tag) extends Table[SchemeClaimRow](tag, "scheme_claim") {

    def empref = column[String]("empref", O.PrimaryKey)

    def dasUserId = column[Long]("das_user_id")

    def accessToken = column[String]("access_token")

    def validUntil = column[Long]("valid_until")

    def refreshToken = column[String]("refresh_token")

    def * = (empref, dasUserId, accessToken, validUntil, refreshToken) <>(SchemeClaimRow.tupled, SchemeClaimRow.unapply)
  }

  def schema = SchemeClaims.schema

}

@Singleton
class SchemeClaimDAO @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)(implicit val ec: ExecutionContext) extends SchemeClaimModule