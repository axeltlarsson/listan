name := """listan-server"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
  jdbc,
  cache,
  ws,
  "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.1" % Test,
  "com.pauldijou" %% "jwt-play" % "0.8.0",
  "com.pauldijou" %% "jwt-play-json" % "0.8.0",
  "org.julienrf" %% "play-json-derived-codecs" % "3.3",
  "com.typesafe.akka" %% "akka-testkit" % "2.4.9"
)

resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"
