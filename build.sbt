name := "command-line-reader"
ThisBuild / version := "1.1.0"
ThisBuild / versionScheme := Some("early-semver")

scalaVersion := "2.13.10"

libraryDependencies ++= Seq(
  "org.scala-lang" % "scala-reflect" % scalaVersion.value,
  "org.scalatest" %% "scalatest" % "3.2.15" % Test
)

ThisBuild / organization := "io.github.krzys9876"
ThisBuild / organizationName := "krzys9876"
ThisBuild / organizationHomepage := Some(url("https://github.com/krzys9876"))
ThisBuild / scmInfo := Some(
  ScmInfo(
    url("https://github.com/krzys9876/command-line-reader"),
    "scm:git@github.com:krzys9876/command-line-reader.git"))
ThisBuild / developers := List(
  Developer(
    id = "krzys9876",
    name = "Krzysztof Ruta",
    email = "krzys9876@gmail.com",
    url = url("https://github.com/krzys9876")))
ThisBuild / description := "Read command line arguments as strongly typed class fields."
ThisBuild / licenses := List("MIT" -> new URL("https://opensource.org/license/mit/"))
ThisBuild / homepage := Some(url("https://github.com/krzys9876/command-line-reader"))

// Remove all additional repository other than Maven Central from POM
ThisBuild / pomIncludeRepository := { _ => false }
ThisBuild / publishTo := {
  val nexus = "https://s01.oss.sonatype.org/"
  if (isSnapshot.value) Some("snapshots" at nexus + "content/repositories/snapshots")
  else Some("releases" at nexus + "service/local/staging/deploy/maven2")
}
ThisBuild / publishMavenStyle := true
