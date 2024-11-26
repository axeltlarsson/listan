name := """listan"""

version := "2.2.1"

packageDescription := """This is the backend to listan. It is built on top of Scala Play Framework."""

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.12.8"

libraryDependencies ++= Seq(
  ehcache,
  ws,
  "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.+" % Test,
  "com.pauldijou" %% "jwt-play" % "2.1.+",
  "com.pauldijou" %% "jwt-play-json" % "2.1.+",
  "org.julienrf" %% "play-json-derived-codecs" % "5.0.+",
  "com.typesafe.akka" %% "akka-testkit" % "2.5.+",
  "org.mockito" % "mockito-core" % "2.18.+" % Test,
  "com.typesafe.play" %% "play-slick" % "4.0.+",
  "com.typesafe.play" %% "play-slick-evolutions" % "4.0.+",
  "org.postgresql" % "postgresql" % "42.2.+",
  "com.github.t3hnar" %% "scala-bcrypt" % "4.+",
  filters,
  guice,
)

resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"

javaOptions in Test += "-Dconfig.file=conf/application.test.conf"
javaOptions in Production += "-Dconfig.file=conf/application.conf" // for 'sbt runProd'
javaOptions in Universal ++= Seq(
  s"-Dpidfile.path=/dev/null",
  s"-Dconfig.file=/usr/share/${packageName.value}/conf/application.conf")
