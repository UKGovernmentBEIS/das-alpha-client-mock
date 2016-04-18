package data

import com.google.inject.ImplementedBy
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat

import scala.concurrent.Future


case class SchemeClaim(empref: String, userId: Long, accessToken: String, validUntil: Long, refreshToken: String) {
  def isAuthTokenExpired: Boolean = new DateTime(validUntil).isBeforeNow
  val validUntilString: String = DateTimeFormat.forPattern("dd/MM/yyyy HH:mm:ss ZZ").print(validUntil)
}


trait SchemeClaimOps {

  def all(): Future[Seq[SchemeClaim]]

  def forUser(userId: Long): Future[Seq[SchemeClaim]]

  def forEmpref(empref: String): Future[Option[SchemeClaim]]

  def updateClaim(row: SchemeClaim): Future[Int]

  def removeClaimForUser(empref: String, userId: Long): Future[Int]

  def removeAllClaimsForUser(userId: Long): Future[Int]

  def insert(cat: SchemeClaim): Future[Unit]

  def expireToken(token: String): Future[Int]
}

