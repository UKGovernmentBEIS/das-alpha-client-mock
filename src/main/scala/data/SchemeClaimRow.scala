package data

import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat

import scala.concurrent.Future


case class SchemeClaimRow(empref: String, userId: UserId, accessToken: String, validUntil: Long, refreshToken: String) {
  def isAuthTokenExpired: Boolean = new DateTime(validUntil).isBeforeNow
  val validUntilString: String = DateTimeFormat.forPattern("dd/MM/yyyy HH:mm:ss ZZ").print(validUntil)
}


trait SchemeClaimOps {

  def all(): Future[Seq[SchemeClaimRow]]

  def forUser(userId: UserId): Future[Seq[SchemeClaimRow]]

  def forEmpref(empref: String): Future[Option[SchemeClaimRow]]

  def updateClaim(row: SchemeClaimRow): Future[Int]

  def removeClaimForUser(empref: String, userId: UserId): Future[Int]

  def removeAllClaimsForUser(userId: UserId): Future[Int]

  def insert(cat: SchemeClaimRow): Future[Unit]

  def expireToken(token: String): Future[Int]
}

