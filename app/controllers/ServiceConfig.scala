package controllers

import javax.inject.{Inject, Singleton}

import play.api.Configuration


@Singleton
class ServiceConfig @Inject()(config: Configuration) {
  val taxserviceBaseURI = config.getString("taxservice.baseURI").getOrElse("http://localhost:9002")

  val apiHost = config.getString("api.host").getOrElse("http://localhost:9001")
  val apiBaseURI = apiHost + "/apprenticeship-levy/epaye/empref"

  val accessTokenUri = s"$taxserviceBaseURI/oauth/token"
  val authorizeSchemeUri = s"$taxserviceBaseURI/oauth/authorize"

  val clientId = config.getString("client.id").getOrElse("client1")
  val clientSecret = config.getString("client.secret").getOrElse("secret1")

  val useSSL = config.getBoolean("client.useSSL").getOrElse(false)
}
