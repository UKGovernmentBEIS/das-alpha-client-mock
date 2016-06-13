name := """das-alpha-client-mock"""

enablePlugins(PlayScala)
enablePlugins(GitVersioning)
enablePlugins(GitBranchPrompt)

git.useGitDescribe := true

routesImport += "uk.gov.hmrc.domain._, models.PlayBindings._"

scalaVersion := "2.11.8"

PlayKeys.devSettings := Seq("play.server.http.port" -> "9000")

resolvers += Resolver.bintrayRepo("hmrc", "releases")

libraryDependencies ++= Seq(
  cache,
  ws,
  "joda-time" % "joda-time" % "2.7",
  "org.joda" % "joda-convert" % "1.7",
  "uk.gov.hmrc" %% "domain" % "3.5.0",
  "com.typesafe.play" %% "play-slick" % "2.0.0",
  "com.typesafe.play" %% "play-slick-evolutions" % "2.0.0",
  "com.h2database" % "h2" % "1.4.191",
  "org.postgresql" % "postgresql" % "9.4.1208",
  "org.mindrot" % "jbcrypt" % "0.3m",
  "org.typelevel" %% "cats" % "0.4.0",
  "com.github.melrief" %% "pureconfig" % "0.1.6",
  "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.0-RC1" % Test
)


