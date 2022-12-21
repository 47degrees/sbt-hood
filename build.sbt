ThisBuild / scalaVersion := "2.13.10"
ThisBuild / organization := "com.47deg"

addCommandAlias("ci-test", "scalafmtCheckAll; scalafmtSbtCheck; mdoc; testCovered")
addCommandAlias("ci-docs", "github; mdoc; headerCreateAll; publishMicrosite")
addCommandAlias("ci-publish", "github; ci-release")

publish / skip := true

lazy val `sbt-hood-core` = project
  .in(file("modules/core"))

lazy val `sbt-hood-plugin` = project
  .in(file("modules/plugin"))
  .enablePlugins(SbtPlugin)
  .dependsOn(`sbt-hood-core`)

lazy val microsite = project
  .enablePlugins(MicrositesPlugin)
  .settings(micrositeSettings: _*)
  .settings(publish / skip := true)

lazy val documentation = project
  .enablePlugins(MdocPlugin)
  .settings(mdocOut := file("."))
  .settings(publish / skip := true)
