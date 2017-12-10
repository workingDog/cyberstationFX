import sbt._
import Keys._

name := "cyberstation"

scalaVersion := "2.12.4"

version := (version in ThisBuild).value

addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full)

libraryDependencies ++= Seq(
  "com.google.inject" % "guice" % "4.1.0",
  "com.jfoenix" % "jfoenix" % "1.9.1",
  "org.scalafx" %% "scalafx" % "8.0.144-R12",
  "org.scalafx" %% "scalafxml-core-sfx8" % "0.4",
  "com.typesafe.play" %% "play-ahc-ws-standalone" % "1.1.3" % "provided",
  "com.github.workingDog" %% "scalastix" % "0.7",
  "com.github.workingDog" %% "taxii2lib" % "0.2",
  "com.typesafe" % "config" % "1.3.2",
  "org.reactivemongo" %% "reactivemongo" % "0.12.7",
  "org.reactivemongo" %% "reactivemongo-play-json" % "0.12.7-play26",
  "org.neo4j" % "neo4j" % "3.3.1" % "provided",
  // "ch.qos.logback" % "logback-classic" % "1.2.3",
  // "com.typesafe.scala-logging" %% "scala-logging" % "3.7.2"
  "org.slf4j" % "slf4j-nop" % "1.7.25"
)

homepage := Some(url("https://github.com/workingDog/cyberstationFX"))

licenses := Seq("Apache 2" -> url("http://www.apache.org/licenses/LICENSE-2.0"))

scalacOptions ++= Seq("-deprecation", "-feature", "-unchecked", "-Xlint")

// Fork a new JVM for 'run' and 'test:run', to avoid JavaFX double initialization problems
fork := true

assemblyMergeStrategy in assembly := {
  case PathList(xs @_*) if xs.last.toLowerCase endsWith ".dsa" => MergeStrategy.discard
  case PathList(xs @_*) if xs.last.toLowerCase endsWith ".sf" => MergeStrategy.discard
  case PathList(xs @_*) if xs.last.toLowerCase endsWith ".des" => MergeStrategy.discard
  case PathList(xs @_*) if xs.last endsWith "LICENSES.txt"=> MergeStrategy.discard
  case PathList(xs @_*) if xs.last.toLowerCase endsWith ".fxml" => MergeStrategy.concat
  case "application.conf" => MergeStrategy.concat
  case x =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
}

assemblyJarName in assembly := "CyberStationApp.jar"

mainClass in assembly := Some("cyber.CyberStationApp")

mainClass in(Compile, run) := Some("cyber.CyberStationApp")

