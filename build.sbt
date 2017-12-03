name := """listan"""

version := "2.0.0"

packageDescription := """This is the backend to listan. It is built on top of Scala Play Framework."""

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.12.4"

libraryDependencies ++= Seq(
  ehcache,
  ws,
  "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.+" % Test,
  "com.pauldijou" %% "jwt-play" % "0.14.+",
  "com.pauldijou" %% "jwt-play-json" % "0.14.+",
  "org.julienrf" %% "play-json-derived-codecs" % "4.0.+",
  "com.typesafe.akka" %% "akka-testkit" % "2.5.+",
  "org.mockito" % "mockito-core" % "2.10.+" % Test,
  "com.typesafe.play" %% "play-slick" % "3.0.+",
  "com.typesafe.play" %% "play-slick-evolutions" % "3.0.+",
  "org.postgresql" % "postgresql" % "42.1.4",
  "com.github.t3hnar" %% "scala-bcrypt" % "3.+",
  filters,
  guice,
)

resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"

javaOptions in Test += "-Dconfig.file=conf/application.test.conf"
javaOptions in Production += "-Dconfig.file=conf/application.prod.conf" // for 'sbt runProd'
javaOptions in Universal ++= Seq(
  s"-Dpidfile.path=/dev/null",
  s"-Dconfig.file=/usr/share/${packageName.value}/conf/application.prod.conf")
