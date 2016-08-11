package db

import com.google.inject.Inject
import data.{StashedTokenDetails, TransientAccessTokenOps}
import play.api.db.slick.DatabaseConfigProvider

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random

trait TransientAccessTokenModule extends SlickModule {

  import driver.api._

  val transientAccessTokens = TableQuery[TransientAccessTokenTable]

  class TransientAccessTokenTable(tag: Tag) extends Table[StashedTokenDetails](tag, "transient_access_token_details") {
    def ref = column[Long]("ref")

    def userId = column[Long]("user_id")

    def empref = column[String]("empref")

    def accessToken = column[String]("access_token")

    def validUntil = column[Long]("valid_until")

    def refreshToken = column[String]("refresh_token")

    def * = (empref, accessToken, validUntil, refreshToken, userId, ref) <> (StashedTokenDetails.tupled, StashedTokenDetails.unapply)
  }

  def schema = transientAccessTokens.schema
}

class TransientAccessTokenDAO @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)(implicit val ec: ExecutionContext)
  extends TransientAccessTokenModule with TransientAccessTokenOps {

  import driver.api._

  override def stash(details: Seq[StashedTokenDetails]): Future[Long] = db.run {
    val ref = Random.nextLong()
    (transientAccessTokens ++= details.map(_.copy(ref = ref))).map(_ => ref)
  }

  /**
    * Pull back the details associated with the id (if there are any). Will remove
    * the details as well, so you can't use the id again.
    */
  override def unstash(ref: Long): Future[Seq[StashedTokenDetails]] = db.run {
    transientAccessTokens.filter(_.ref === ref).result
  }.map { atd =>
    atd.foreach(_ => transientAccessTokens.filter(_.ref === ref).delete)
    atd
  }
}
