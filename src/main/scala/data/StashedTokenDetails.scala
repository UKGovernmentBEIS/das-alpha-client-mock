package data

import scala.concurrent.Future

case class StashedTokenDetails(empref: String, accessToken: String, validUntil: Long, refreshToken: String, userId: Long, ref: Long=0L)

trait TransientAccessTokenOps {
  /**
    * Store the details and return an id you can use to fetch them again
    */
  def stash(details: Seq[StashedTokenDetails]): Future[Long]

  /**
    * Pull back the details associated with the id (if there are any). Will remove
    * the details as well, so you can't use the id again.
    */
  def unstash(id: Long): Future[Seq[StashedTokenDetails]]
}