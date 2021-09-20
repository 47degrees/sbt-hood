ThisBuild / scalaVersion := "2.13.6"
ThisBuild / organization := "com.47deg"

addCommandAlias("ci-test", "scalafmtCheckAll; scalafmtSbtCheck; mdoc; testCovered")
addCommandAlias("ci-docs", "github; mdoc; headerCreateAll; publishMicrosite")
addCommandAlias("ci-publish", "github; ci-release")

skip in publish := true

lazy val `sbt-hood-core` = project
  .in(file("modules/core"))

lazy val `sbt-hood-plugin` = project
  .in(file("modules/plugin"))
  .enablePlugins(SbtPlugin)
  .dependsOn(`sbt-hood-core`)

lazy val microsite = project
  .enablePlugins(MicrositesPlugin)
  .settings(micrositeSettings: _*)
  .settings(skip in publish := true)

lazy val documentation = project
  .enablePlugins(MdocPlugin)
  .settings(mdocOut := file("."))
  .settings(skip in publish := true)
