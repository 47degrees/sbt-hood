addCommandAlias(
  "ci-test",
  "+scalafmtCheckAll; +scalafmtSbtCheck; +docs/mdoc; +coverage; +test; +coverageReport; +coverageAggregate"
)
addCommandAlias("ci-docs", "project-docs/mdoc; headerCreateAll")
addCommandAlias("ci-microsite", "docs/publishMicrosite")

skip in publish := true

lazy val `sbt-hood-core` = project
  .in(file("modules/core"))

lazy val `sbt-hood-plugin` = project
  .in(file("modules/plugin"))
  .dependsOn(`sbt-hood-core`)
  .enablePlugins(BuildInfoPlugin)
  .settings(buildInfoPackage := "com.fortysevendeg.hood")
  .enablePlugins(SbtPlugin)

lazy val microsite = project
  .enablePlugins(MicrositesPlugin)
  .settings(micrositeSettings: _*)
  .settings(skip in publish := true)

lazy val documentation = project
  .enablePlugins(MdocPlugin)
  .settings(mdocOut := file("."))
  .settings(skip in publish := true)
