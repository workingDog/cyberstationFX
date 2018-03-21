import sbt._
import Keys._

name := "cyberstation"

scalaVersion := "2.12.5"

version := (version in ThisBuild).value

addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full)

resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"

libraryDependencies ++= Seq(
  "org.neo4j" % "neo4j" % "3.3.3",
  "com.typesafe.play" %% "play-ahc-ws-standalone" % "1.1.6",
//  "com.typesafe.play" %% "play-ws-standalone-json" % "1.1.3",
  "com.github.workingDog" %% "scalastix" % "0.7",
  "com.github.workingDog" %% "taxii2lib" % "0.3",
  "com.github.workingDog" %% "stixtoneolib" % "0.3",
  "org.reactivemongo" %% "reactivemongo" % "0.13.0",
  "org.reactivemongo" %% "reactivemongo-play-json" % "0.13.0-play26",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.8.0"
).map(_.exclude("org.slf4j", "*"))

libraryDependencies ++= Seq(
  "org.scala-lang.modules" %% "scala-xml" % "1.1.0",
  "com.typesafe" % "config" % "1.3.3",
  "com.google.inject" % "guice" % "4.2.0",
  "com.jfoenix" % "jfoenix" % "8.0.3",
  "org.scalafx" %% "scalafx" % "8.0.144-R12",
  "org.scalafx" %% "scalafxml-core-sfx8" % "0.4",
  "ch.qos.logback" % "logback-classic" % "1.2.3"
)


homepage := Some(url("https://github.com/workingDog/cyberstationFX"))

licenses := Seq("Apache 2" -> url("http://www.apache.org/licenses/LICENSE-2.0"))

scalacOptions ++= Seq("-deprecation", "-feature", "-unchecked", "-Xlint")

// Fork a new JVM for 'run' and 'test:run', to avoid JavaFX double initialization problems
fork := true

assemblyMergeStrategy in assembly := {
  case PathList(xs@_*) if xs.last.toLowerCase endsWith ".rsa" => MergeStrategy.discard
  case PathList(xs@_*) if xs.last.toLowerCase endsWith ".dsa" => MergeStrategy.discard
  case PathList(xs@_*) if xs.last.toLowerCase endsWith ".sf" => MergeStrategy.discard
  case PathList(xs@_*) if xs.last.toLowerCase endsWith ".des" => MergeStrategy.discard
  case PathList(xs@_*) if xs.last endsWith "LICENSES.txt" => MergeStrategy.discard
  case PathList(xs@_*) if xs.last endsWith "LICENSE.txt" => MergeStrategy.discard
  case PathList(xs@_*) if xs.last endsWith "logback.xml" => MergeStrategy.discard
  case PathList(xs @_*) if xs.last.toLowerCase endsWith ".fxml" => MergeStrategy.concat
  case "META-INF/io.netty.versions.properties" => MergeStrategy.discard
  case "application.conf" => MergeStrategy.concat
  case x =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
}

assemblyJarName in assembly := "cyberstation-" + version.value + ".jar"

mainClass in assembly := Some("cyber.CyberStationApp")

mainClass in(Compile, run) := Some("cyber.CyberStationApp")

