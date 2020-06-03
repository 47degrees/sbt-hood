ThisBuild / scalaVersion := "2.12.8"
ThisBuild / organization := "com.47deg"

addCommandAlias(
  "ci-test",
  "scalafmtCheckAll; scalafmtSbtCheck; mdoc; coverage; test; coverageReport; coverageAggregate"
)
addCommandAlias("ci-docs", "mdoc; headerCreateAll")
addCommandAlias("ci-microsite", "publishMicrosite")

skip in publish := true

lazy val `sbt-hood-core` = project
  .in(file("modules/core"))

lazy val `sbt-hood-plugin` = project
  .in(file("modules/plugin"))
  .enablePlugins(SbtPlugin)
  .dependsOn(`sbt-hood-core`)
  .enablePlugins(BuildInfoPlugin)
  .settings(buildInfoPackage := "com.fortysevendeg.hood")

lazy val microsite = project
  .enablePlugins(MicrositesPlugin)
  .settings(micrositeSettings: _*)
  .settings(skip in publish := true)

lazy val documentation = project
  .enablePlugins(MdocPlugin)
  .settings(mdocOut := file("."))
  .settings(skip in publish := true)
