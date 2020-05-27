import com.typesafe.sbt.site.jekyll.JekyllPlugin.autoImport._
import microsites.MicrositeKeys._
import sbt.Keys._
import sbt.ScriptedPlugin.autoImport._
import sbt._
import scoverage.ScoverageKeys._
import com.alejandrohdezma.sbt.github.SbtGithubPlugin

import scala.language.reflectiveCalls

object ProjectPlugin extends AutoPlugin {

  override def trigger: PluginTrigger = allRequirements

  override def requires: Plugins = SbtGithubPlugin

  object autoImport {

    lazy val V = new {
      val scala: String          = "2.12.8"
      val scalatest: String      = "3.1.2"
      val slf4j: String          = "1.7.30"
      val circe: String          = "0.13.0"
      val github4s: String       = "0.23.0"
      val cats: String           = "2.1.3"
      val log4cats: String       = "1.1.1"
      val logbackClassic: String = "1.2.3"
      val kantan: String         = "0.6.1"
      val console4cats: String   = "0.8.1"
      val lightbendEmoji: String = "1.2.1"
    }

    lazy val sbtPluginSettings: Seq[Def.Setting[_]] = Seq(
      scriptedLaunchOpts := {
        scriptedLaunchOpts.value ++
          Seq(
            "-Xmx2048M",
            "-XX:ReservedCodeCacheSize=256m",
            "-XX:+UseConcMarkSweepGC",
            "-Dversion=" + version.value
          )
      }
    )

    lazy val micrositeSettings: Seq[Def.Setting[_]] = Seq(
      micrositeName := "SBT-Hood",
      micrositeBaseUrl := "/sbt-hood",
      micrositeDescription := "A SBT plugin for comparing benchmarks in your PRs",
      micrositeGithubOwner := "47degrees",
      micrositeGithubRepo := "sbt-hood",
      micrositeDocumentationUrl := "docs",
      micrositeGitterChannelUrl := "47deg/sbthood",
      micrositeOrganizationHomepage := "http://www.47deg.com",
      includeFilter in Jekyll := "*.html" | "*.css" | "*.png" | "*.jpg" | "*.gif" | "*.js" | "*.swf" | "*.md" | "*.svg",
      micrositePushSiteWith := GitHub4s,
      micrositeHighlightTheme := "atom-one-light",
      micrositeGithubToken := Option(System.getenv().get("GITHUB_TOKEN")),
      micrositePalette := Map(
        "brand-primary"   -> "#25bc77",
        "brand-secondary" -> "#25bc77",
        "white-color"     -> "#FFF"
      ),
      scalacOptions ~= (_ filterNot Set("-Ywarn-unused-import", "-Xlint").contains)
    )

  }

  import autoImport._

  override def projectSettings: Seq[Def.Setting[_]] =
    Seq(
      organization := "com.47deg",
      scalaVersion := V.scala,
      crossScalaVersions := Seq(V.scala),
      Test / fork := true,
      compileOrder in Compile := CompileOrder.JavaThenScala,
      coverageFailOnMinimum := false,
      resolvers ++= Seq(
        Resolver.sonatypeRepo("releases"),
        Resolver.sonatypeRepo("snapshots"),
        Resolver.typesafeIvyRepo("releases")
      ),
      libraryDependencies ++= Seq(
        "io.circe"          %% "circe-generic"      % V.circe,
        "io.circe"          %% "circe-core"         % V.circe,
        "io.circe"          %% "circe-parser"       % V.circe,
        "com.47deg"         %% "github4s"           % V.github4s,
        "org.typelevel"     %% "cats-effect"        % V.cats,
        "io.chrisdavenport" %% "log4cats-slf4j"     % V.log4cats,
        "ch.qos.logback"     % "logback-classic"    % V.logbackClassic,
        "com.nrinaudo"      %% "kantan.csv"         % V.kantan,
        "com.nrinaudo"      %% "kantan.csv-generic" % V.kantan,
        "dev.profunktor"    %% "console4cats"       % V.console4cats,
        "com.lightbend"     %% "emoji"              % V.lightbendEmoji,
        "org.scalatest"     %% "scalatest"          % V.scalatest % Test,
        "org.slf4j"          % "slf4j-nop"          % V.slf4j     % Test
      )
    )
}
