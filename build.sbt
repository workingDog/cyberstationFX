name := "cyberstation"

version := "0.1"

scalaVersion := "2.12.3"

version := (version in ThisBuild).value

addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full)

resourceDirectory in Compile := (scalaSource in Compile).value

libraryDependencies ++= Seq(
  "com.google.inject" % "guice" % "4.1.0",
  "com.jfoenix" % "jfoenix" % "1.9.1",
  "org.scalafx" %% "scalafx" % "8.0.144-R12",
  "org.scalafx" %% "scalafxml-core-sfx8" % "0.4",
  "com.typesafe.play" %% "play-ahc-ws-standalone" % "1.1.2",
  "com.typesafe.akka" %% "akka-actor" % "2.5.6",
  "com.github.workingDog" %% "taxii2lib" % "0.1"
)

homepage := Some(url("https://github.com/workingDog/CyberStationApp"))

licenses := Seq("Apache 2" -> url("http://www.apache.org/licenses/LICENSE-2.0"))

scalacOptions ++= Seq(
  //  "-Ypartial-unification", // to improves type constructor inference
  "-deprecation", // Emit warning and location for usages of deprecated APIs.
  "-feature", // Emit warning and location for usages of features that should be imported explicitly.
  "-unchecked", // Enable additional warnings where generated code depends on assumptions.
  "-Xlint" // Enable recommended additional warnings.
)

// Fork a new JVM for 'run' and 'test:run', to avoid JavaFX double initialization problems
fork := true
