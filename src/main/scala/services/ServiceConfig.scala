package services

import scala.util.{Failure, Success}

case class ServiceConfig(taxservice: TaxServiceConfig, api: ApiConfig, client: ClientConfig)

case class ClientConfig(id: String, secret: String, useSSL: Boolean)

case class ApiConfig(host: String) {
  val baseURI = host + "/apprenticeship-levy/epaye"
}

case class TaxServiceConfig(baseURI: String, callbackURL: String) {
  val accessTokenUri = s"$baseURI/oauth/token"
  val authorizeSchemeUri = s"$baseURI/oauth/authorize"
}

object ServiceConfig {

  import pureconfig._

  lazy val config = loadConfig[ServiceConfig] match {
    case Success(c) => c
    case Failure(t) => throw t
  }
}
