package services

case class ServiceConfig(taxservice: TaxServiceConfig, api: ApiConfig, client: ClientConfig)

case class ClientConfig(id: String, secret: String, useSSL: Boolean)

case class ApiConfig(host: String) {
  val baseURI = host + "/apprenticeship-levy/epaye"
}

case class TaxServiceConfig(baseURI: String) {
  val accessTokenUri = s"$baseURI/oauth/token"
  val authorizeSchemeUri = s"$baseURI/oauth/authorize"
}


object ServiceConfig {
  import pureconfig._

  lazy val config = loadConfig[ServiceConfig].get
}
