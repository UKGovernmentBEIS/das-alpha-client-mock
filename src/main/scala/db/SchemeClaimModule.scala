package db

import javax.inject.{Inject, Singleton}

import data.{SchemeClaim, SchemeClaimOps}
import play.api.db.slick.DatabaseConfigProvider

import scala.concurrent.{ExecutionContext, Future}

trait SchemeClaimModule extends SlickModule {

  import driver.api._

  val SchemeClaims = TableQuery[SchemeClaimTable]

  class SchemeClaimTable(tag: Tag) extends Table[SchemeClaim](tag, "scheme_claim") {

    def empref = column[String]("empref", O.PrimaryKey)

    def dasUserId = column[Long]("das_user_id")

    def accessToken = column[String]("access_token")

    def validUntil = column[Long]("valid_until")

    def refreshToken = column[String]("refresh_token")

    def * = (empref, dasUserId, accessToken, validUntil, refreshToken) <>(SchemeClaim.tupled, SchemeClaim.unapply)
  }

  def schema = SchemeClaims.schema

}

@Singleton
class SchemeClaimDAO @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)(implicit val ec: ExecutionContext)
  extends SchemeClaimModule with SchemeClaimOps {

  import driver.api._

  def all(): Future[Seq[SchemeClaim]] = db.run(SchemeClaims.result)

  def forUser(userId: Long): Future[Seq[SchemeClaim]] = db.run(SchemeClaims.filter(_.dasUserId === userId).result)

  def forEmpref(empref: String): Future[Option[SchemeClaim]] = db.run(SchemeClaims.filter(_.empref === empref).result.headOption)

  def updateClaim(row: SchemeClaim): Future[Int] = db.run {
    SchemeClaims.filter(_.empref === row.empref).update(row)
  }

  def removeClaimForUser(empref: String, userId: Long): Future[Int] = db.run {
    SchemeClaims.filter(sc => sc.empref === empref && sc.dasUserId === userId).delete
  }

  def removeAllClaimsForUser(userId: Long): Future[Int] = db.run(SchemeClaims.filter(_.dasUserId === userId).delete)

  def insert(cat: SchemeClaim): Future[Unit] = db.run(SchemeClaims += cat).map { _ => () }

  override def expireToken(token: String): Future[Int] = db.run {
    val q = for {
      c <- SchemeClaims if c.accessToken === token
    } yield c.validUntil

    q.update(System.currentTimeMillis())
  }
}