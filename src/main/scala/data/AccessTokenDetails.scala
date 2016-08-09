package data

import scala.concurrent.Future

case class AccessTokenDetails(accessToken: String, validUntil: Long, refreshToken: String, id: Long = 0)

trait TransientAccessTokenOps {
  /**
    * Store the details and return an id you can use to fetch them again
    */
  def stash(details: AccessTokenDetails): Future[Long]

  /**
    * Pull back the details associated with the id (if there are any). Will remove
    * the details as well, so you can't use the id again.
    */
  def unstash(id: Long): Future[Option[AccessTokenDetails]]
}