package db

import com.google.inject.Inject
import data.{StashedTokenDetails, TokenStashOps}
import play.api.db.slick.DatabaseConfigProvider

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random

trait TokenStashModule extends SlickModule {

  import driver.api._

  val tokenStash = TableQuery[TokenStashTable]

  class TokenStashTable(tag: Tag) extends Table[StashedTokenDetails](tag, "token_stash") {
    def ref = column[Long]("ref")

    def userId = column[Long]("user_id")

    def empref = column[String]("empref")

    def accessToken = column[String]("access_token")

    def validUntil = column[Long]("valid_until")

    def refreshToken = column[String]("refresh_token")

    def * = (empref, accessToken, validUntil, refreshToken, userId, ref) <> (StashedTokenDetails.tupled, StashedTokenDetails.unapply)
  }

  def schema = tokenStash.schema
}

class TokenStashDAO @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)(implicit val ec: ExecutionContext)
  extends TokenStashModule with TokenStashOps {

  import driver.api._

  override def stash(details: Seq[StashedTokenDetails]): Future[Long] = db.run {
    val ref = Random.nextInt().abs
    (tokenStash ++= details.map(_.copy(ref = ref))).map(_ => ref)
  }

  /**
    * retrieve stashed details associated with id, but leave them in place
    */
  override def peek(ref: Long): Future[Seq[StashedTokenDetails]] = db.run {
    tokenStash.filter(_.ref === ref).result
  }

  /**
    * Pull back the details associated with the id (if there are any). Will remove
    * the details as well, so you can't use the id again.
    */
  override def unstash(ref: Long): Future[Seq[StashedTokenDetails]] = db.run {
    tokenStash.filter(_.ref === ref).result
  }.map { atd =>
    atd.foreach(_ => tokenStash.filter(_.ref === ref).delete)
    atd
  }

  /**
    * Remove the stashed details associated with the ref
    */
  override def drop(ref: Long): Future[Int] = db.run {
    tokenStash.filter(_.ref === ref).delete
  }
}
