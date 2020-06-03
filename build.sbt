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

lazy val allModules: Seq[ProjectReference] = Seq(`sbt-hood-core`, `sbt-hood-plugin`)

lazy val allModulesDeps: Seq[ClasspathDependency] =
  allModules.map(ClasspathDependency(_, None))

lazy val docs = project
  .dependsOn(allModulesDeps: _*)
  .settings(name := "sbt-hood-docs")
  .settings(micrositeSettings: _*)
  .settings(skip in publish := true)
  .enablePlugins(MicrositesPlugin)

lazy val `project-docs` = (project in file(".docs"))
  .aggregate(allModules: _*)
  .dependsOn(allModulesDeps: _*)
  .settings(moduleName := "sbt-hood-project-docs")
  .settings(mdocIn := file(".docs"))
  .settings(mdocOut := file("."))
  .settings(skip in publish := true)
  .enablePlugins(MdocPlugin)
