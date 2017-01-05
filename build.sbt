name := "das-alpha-client-mock"

lazy val `das-alpha-client-mock` = (project in file("."))
  .enablePlugins(PlayScala)
  .disablePlugins(PlayLayoutPlugin)
  .enablePlugins(GitVersioning)
  .enablePlugins(GitBranchPrompt)

git.useGitDescribe := true

routesImport ++= Seq(
  "uk.gov.hmrc.domain._",
  "data._",
  "models.PlayBindings._",
  "com.wellfactored.playbindings.ValueClassUrlBinders._"
)
scalaVersion := "2.11.8"

PlayKeys.devSettings := Seq("play.server.http.port" -> "9000")

javaOptions := Seq(
  "-Dconfig.file=src/main/resources/development.application.conf",
  "-Dlogger.file=src/main/resources/development.logger.xml"
)

resolvers += Resolver.bintrayRepo("hmrc", "releases")

// need this because we've disabled the PlayLayoutPlugin. without it twirl templates won't get
// re-compiled on change in dev mode
PlayKeys.playMonitoredFiles ++= (sourceDirectories in(Compile, TwirlKeys.compileTemplates)).value

libraryDependencies ++= Seq(
  ws,
  "joda-time" % "joda-time" % "2.7",
  "org.joda" % "joda-convert" % "1.7",
  "uk.gov.hmrc" %% "domain" % "3.5.0",
  "com.typesafe.play" %% "play-slick" % "2.0.0",
  "com.typesafe.play" %% "play-slick-evolutions" % "2.0.0",
  "org.postgresql" % "postgresql" % "9.4.1208",
  "org.mindrot" % "jbcrypt" % "0.3m",
  "com.wellfactored" %% "play-bindings" % "2.0.0",
  "org.typelevel" %% "cats-core" % "0.8.1",
  "com.github.melrief" %% "pureconfig" % "0.1.6",
  "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.0-RC1" % Test
)


