name := "args"

version := "0.1"

scalaVersion := "2.13.8"

idePackagePrefix := Some("org.kr.args")

val coreDependencies = Seq(
  "org.scala-lang" % "scala-reflect" % scalaVersion.toString()
)

val testDependencies = Seq(
  "org.scalatest" %% "scalatest" % "3.2.12" % Test
)

libraryDependencies ++= coreDependencies ++ testDependencies

//https://www.scalatest.org/user_guide/using_the_runner
// redirect  test results to a file
Test / testOptions += Tests.Argument("-fW", "target/test-report.txt")
// parallel execution makes test report unordered and less readable
Test / parallelExecution := false

// from: https://githubhelp.com/sbt/sbt-assembly
Compile / run := Defaults.runTask(Compile / fullClasspath, Compile / run / mainClass, Compile / run / runner).evaluated
Compile / runMain := Defaults.runMainTask(Compile / fullClasspath, Compile / run / runner).evaluated
ThisBuild / scalacOptions ++= Seq("-deprecation", "-feature")

