package controllers

import javax.inject.{Inject, Singleton}

import play.api.Configuration


@Singleton
class ServiceConfig @Inject()(config: Configuration) {
  val taxserviceBaseURI = config.getString("taxservice.baseURI").get

  val apiBaseURI = config.getString("api.baseURI").get

  val accessTokenUri = s"$taxserviceBaseURI/oauth/token"
  val authorizeSchemeUri = s"$taxserviceBaseURI/oauth/authorize"

  val clientId = config.getString("client.id").get
  val clientSecret = config.getString("client.secret").get
}
