import sbtorgpolicies.model.scalac

pgpPassphrase := Some(getEnvVar("PGP_PASSPHRASE").getOrElse("").toCharArray)

lazy val `sbt-hood-core` = project
  .in(file("modules/core"))
  .settings(moduleName := "sbt-hood-core")

lazy val `sbt-hood-plugin` = project
  .in(file("modules/plugin"))
  .dependsOn(`sbt-hood-core`)
  .settings(moduleName := "sbt-hood-plugin")
  .settings(crossScalaVersions := Seq(scalac.`2.12`))
  .settings(sbtPluginSettings: _*)
  .enablePlugins(BuildInfoPlugin)
  .settings(buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion))
  .settings(buildInfoPackage := "com.fortysevendeg.hood")
  // See https://github.com/sbt/sbt/issues/3248
  .settings(publishLocal := publishLocal
    .dependsOn(`sbt-hood-core` / publishLocal)
    .value)
  .settings(sbtPlugin := true)
  .enablePlugins(SbtPlugin)

lazy val allModules: Seq[ProjectReference] = Seq(`sbt-hood-core`)

lazy val allModulesDeps: Seq[ClasspathDependency] =
  allModules.map(ClasspathDependency(_, None))

lazy val root = project
  .in(file("."))
  .settings(name := "sbt-hood")
  .settings(noPublishSettings)
  .aggregate(allModules: _*)
  .dependsOn(allModulesDeps: _*)

lazy val docs = project
  .in(file("docs"))
  .dependsOn(allModulesDeps: _*)
  .settings(name := "sbt-hood-docs")
  .settings(docsSettings: _*)
  .settings(micrositeSettings: _*)
  .settings(noPublishSettings: _*)
  .enablePlugins(MicrositesPlugin)