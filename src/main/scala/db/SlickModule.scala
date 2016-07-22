package db

import play.api.db.slick.HasDatabaseConfigProvider
import slick.driver.JdbcProfile

import scala.concurrent.ExecutionContext

trait SlickModule extends HasDatabaseConfigProvider[JdbcProfile] {
  implicit def ec: ExecutionContext
}
