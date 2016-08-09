package db

import com.google.inject.Inject
import data.{AccessTokenDetails, TransientAccessTokenOps}
import play.api.db.slick.DatabaseConfigProvider

import scala.concurrent.{ExecutionContext, Future}

trait TransientAccessTokenModule extends SlickModule {

  import driver.api._

  val transientAccessTokens = TableQuery[TransientAccessTokenTable]

  class TransientAccessTokenTable(tag: Tag) extends Table[AccessTokenDetails](tag, "transient_access_token_details") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)

    def accessToken = column[String]("access_token")

    def validUntil = column[Long]("valid_until")

    def refreshToken = column[String]("refresh_token")

    def * = (accessToken, validUntil, refreshToken, id) <> (AccessTokenDetails.tupled, AccessTokenDetails.unapply)
  }

  def schema = transientAccessTokens.schema
}

class TransientAccessTokenDAO @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)(implicit val ec: ExecutionContext)
  extends TransientAccessTokenModule with TransientAccessTokenOps {

  import driver.api._

  /**
    * Store the details and return an id you can use to fetch them again
    */
  override def stash(details: AccessTokenDetails): Future[Long] = db.run {
    transientAccessTokens returning transientAccessTokens.map(_.id) += details
  }

  /**
    * Pull back the details associated with the id (if there are any). Will remove
    * the details as well, so you can't use the id again.
    */
  override def unstash(id: Long): Future[Option[AccessTokenDetails]] = db.run {
    transientAccessTokens.filter(_.id === id).result.headOption
  }.map { atd =>
    atd.foreach(_ => transientAccessTokens.filter(_.id === id).delete)
    atd
  }
}
