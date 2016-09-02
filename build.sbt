name := """listan-server"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
  cache,
  ws,
  "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.1" % Test,
  "com.pauldijou" %% "jwt-play" % "0.8.0",
  "com.pauldijou" %% "jwt-play-json" % "0.8.0",
  "org.julienrf" %% "play-json-derived-codecs" % "3.3",
  "com.typesafe.akka" %% "akka-testkit" % "2.4.9",
  "org.mockito" % "mockito-core" % "2.0.0-beta.118",
  "com.typesafe.play" %% "play-slick" % "2.0.0",
  "com.typesafe.play" %% "play-slick-evolutions" % "2.0.0",
  "com.h2database" % "h2" % "1.4.192",
  "com.github.t3hnar" %% "scala-bcrypt" % "2.6"
)

resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"

javaOptions in Test += "-Dconfig.file=conf/application.test.conf"
