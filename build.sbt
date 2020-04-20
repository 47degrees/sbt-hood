addCommandAlias(
  "ci-test",
  "+scalafmtCheck; +scalafmtSbtCheck; +docs/mdoc; +coverage; +test; +coverageReport; +coverageAggregate"
)
addCommandAlias("ci-docs", "project-docs/mdoc; headerCreateAll")
addCommandAlias("ci-microsite", "docs/publishMicrosite")

lazy val `sbt-hood-core` = project
  .in(file("modules/core"))
  .settings(moduleName := "sbt-hood-core")

lazy val `sbt-hood-plugin` = project
  .in(file("modules/plugin"))
  .dependsOn(`sbt-hood-core`)
  .settings(moduleName := "sbt-hood-plugin")
  .settings(sbtPluginSettings: _*)
  .enablePlugins(BuildInfoPlugin)
  .settings(buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion))
  .settings(buildInfoPackage := "com.fortysevendeg.hood")
  // See https://github.com/sbt/sbt/issues/3248
  .settings(
    publishLocal := publishLocal
      .dependsOn(`sbt-hood-core` / publishLocal)
      .value
  )
  .settings(sbtPlugin := true)
  .enablePlugins(SbtPlugin)

lazy val allModules: Seq[ProjectReference] = Seq(`sbt-hood-core`, `sbt-hood-plugin`)

lazy val allModulesDeps: Seq[ClasspathDependency] =
  allModules.map(ClasspathDependency(_, None))

lazy val root = project
  .in(file("."))
  .settings(name := "sbt-hood")
  .settings(skip in publish := true)
  .aggregate(allModules: _*)
  .dependsOn(allModulesDeps: _*)

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
