package db

import javax.inject.{Inject, Singleton}

import data._
import play.api.db.slick.DatabaseConfigProvider

import scala.concurrent.{ExecutionContext, Future}

trait SchemeClaimModule extends SlickModule {
  self: DASUserModule =>

  import driver.api._

  implicit val accessTokenMapper: BaseColumnType[AccessToken] = MappedColumnType.base[AccessToken, String](_.token, AccessToken)
  implicit val refreshTokenMapper: BaseColumnType[RefreshToken] = MappedColumnType.base[RefreshToken, String](_.token, RefreshToken)

  val schemeClaims = TableQuery[SchemeClaimTable]

  class SchemeClaimTable(tag: Tag) extends Table[SchemeClaimRow](tag, "scheme_claim") {

    def empref = column[String]("empref", O.PrimaryKey)

    def dasUserId = column[UserId]("das_user_id")

    def accessToken = column[AccessToken]("access_token")

    def validUntil = column[Long]("valid_until")

    def refreshToken = column[RefreshToken]("refresh_token")

    def * = (empref, dasUserId, accessToken, validUntil, refreshToken) <> (SchemeClaimRow.tupled, SchemeClaimRow.unapply)
  }

  def schema = schemeClaims.schema

}

@Singleton
class SchemeClaimDAO @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)(implicit val ec: ExecutionContext)
  extends SchemeClaimModule with SchemeClaimOps with DASUserModule {

  import driver.api._

  def all(): Future[Seq[SchemeClaimRow]] = db.run(schemeClaims.result)

  def forUser(userId: UserId): Future[Seq[SchemeClaimRow]] = db.run(schemeClaims.filter(_.dasUserId === userId).result)

  def forEmpref(empref: String): Future[Option[SchemeClaimRow]] = db.run(schemeClaims.filter(_.empref === empref).result.headOption)

  def updateClaim(row: SchemeClaimRow): Future[Int] = db.run {
    schemeClaims.filter(_.empref === row.empref).update(row)
  }

  def removeClaimForUser(empref: String, userId: UserId): Future[Int] = db.run {
    schemeClaims.filter(sc => sc.empref === empref && sc.dasUserId === userId).delete
  }

  def removeAllClaimsForUser(userId: UserId): Future[Int] = db.run(schemeClaims.filter(_.dasUserId === userId).delete)

  def insert(cat: SchemeClaimRow): Future[Unit] = db.run(schemeClaims += cat).map { _ => () }

  override def expireToken(token: AccessToken): Future[Int] = db.run {
    val q = for {
      c <- schemeClaims if c.accessToken === token
    } yield c.validUntil

    q.update(System.currentTimeMillis())
  }
}