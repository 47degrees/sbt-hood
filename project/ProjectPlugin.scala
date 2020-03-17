import com.typesafe.sbt.site.jekyll.JekyllPlugin.autoImport._
import microsites.MicrositeKeys._
import sbt.Keys._
import sbt.ScriptedPlugin.autoImport._
import sbt._
import sbtorgpolicies.OrgPoliciesPlugin
import sbtorgpolicies.OrgPoliciesPlugin.autoImport._
import sbtorgpolicies.model._
import sbtorgpolicies.templates._
import sbtorgpolicies.templates.badges._
import sbtorgpolicies.runnable.syntax._
import sbtrelease.ReleasePlugin.autoImport._
import scoverage.ScoverageKeys._

import scala.language.reflectiveCalls

object ProjectPlugin extends AutoPlugin {

  override def requires: Plugins = OrgPoliciesPlugin

  override def trigger: PluginTrigger = allRequirements

  object autoImport {

    lazy val V = new {
      val kindProjector: String   = "0.11.0"
      val paradise: String        = "2.1.1"
      val scala: String           = "2.12.8"
      val scalatest: String       = "3.1.1"
      val slf4j: String           = "1.7.30"
      val circe: String           = "0.11.1"
      val github4s: String        = "0.23.0"
      val cats: String            = "2.0.0-M1"
      val log4cats: String        = "0.4.0-M1"
      val logbackClassic: String  = "1.2.3"
      val kantan: String          = "0.6.0"
      val console4cats: String    = "0.8.0-M1"
      val lightbendEmoji: String  = "1.2.1"
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
      micrositeGithubToken := sys.env.get(orgGithubTokenSetting.value),
      micrositePalette := Map(
        "brand-primary"     -> "#25bc77",
        "brand-secondary"   -> "#25bc77",
        "white-color"       -> "#FFF"
      )
    )

    lazy val docsSettings: Seq[Def.Setting[_]] = Seq(
      libraryDependencies += %%("scalatest", V.scalatest),
      scalacOptions ~= (_ filterNot Set("-Ywarn-unused-import", "-Xlint").contains)
    )

  }

  import autoImport._

  override def projectSettings: Seq[Def.Setting[_]] =
    sharedReleaseProcess ++ warnUnusedImport ++ Seq(
      description := "A SBT plugin for comparing benchmarks in your PRs",
      orgGithubSetting := GitHubSettings(
        organization = "47degrees",
        project = (name in LocalRootProject).value,
        organizationName = "47 Degrees",
        groupId = "com.47deg",
        organizationHomePage = url("http://47deg.com"),
        organizationEmail = "hello@47deg.com"
      ),
      startYear := Some(2019),
      orgProjectName := "sbt-hood",
      scalaVersion := V.scala,
      crossScalaVersions := Seq(V.scala),
      scalacOptions ++= scalacAdvancedOptions,
      scalacOptions ~= (_ filterNot Set("-Yliteral-types", "-Xlint").contains),
        Test / fork := true,
      compileOrder in Compile := CompileOrder.JavaThenScala,
      coverageFailOnMinimum := false,
      resolvers ++= Seq(Resolver.sonatypeRepo("releases"), Resolver.sonatypeRepo("snapshots"), Resolver.typesafeIvyRepo("releases")),
      addCompilerPlugin(%%("paradise", V.paradise) cross CrossVersion.full),
      addCompilerPlugin(%%("kind-projector", V.kindProjector) cross CrossVersion.full),
      libraryDependencies ++= Seq(
        "io.circe"                   %% "circe-generic"          % V.circe,
        "io.circe"                   %% "circe-core"             % V.circe,
        "io.circe"                   %% "circe-parser"           % V.circe,
        "com.47deg"                  %% "github4s"               % V.github4s,
        "org.typelevel"              %% "cats-effect"            % V.cats,
        "io.chrisdavenport"          %% "log4cats-slf4j"         % V.log4cats,
        "ch.qos.logback"              % "logback-classic"        % V.logbackClassic,
        "com.nrinaudo"               %% "kantan.csv"             % V.kantan,
        "com.nrinaudo"               %% "kantan.csv-generic"     % V.kantan,
        "dev.profunktor"             %% "console4cats"           % V.console4cats,
        "com.lightbend"              %% "emoji"                  % V.lightbendEmoji,
        %%("scalatest", V.scalatest) % "test",
        %("slf4j-nop", V.slf4j)      % Test
      )
    ) ++ Seq(
      // sbt-org-policies settings:
      // format: OFF
      orgMaintainersSetting := List(Dev("developer47deg", Some("47 Degrees (twitter: @47deg)"), Some("hello@47deg.com"))),
      orgBadgeListSetting := List(
          GitterBadge.apply,
          TravisBadge.apply,
          CodecovBadge.apply,
          MavenCentralBadge.apply,
          LicenseBadge.apply,
          ScalaLangBadge.apply,
          GitHubIssuesBadge.apply
      )
    )
  // format: ON
}
