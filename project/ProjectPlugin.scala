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
import tut.TutPlugin.autoImport._

object ProjectPlugin extends AutoPlugin {

  override def requires: Plugins = OrgPoliciesPlugin

  override def trigger: PluginTrigger = allRequirements

  object autoImport {

    lazy val V = new {
    val scala: String     = "2.12.8"
    val scalatest: String = "3.0.6"
    val slf4j: String     = "1.7.26"
    }

    lazy val idlGenSettings: Seq[Def.Setting[_]] = Seq(
      libraryDependencies ++= Seq()
    )

    lazy val sbtPluginSettings: Seq[Def.Setting[_]] = Seq(
      scriptedLaunchOpts := {
        scriptedLaunchOpts.value ++
          Seq(
            "-Xmx2048M",
            "-XX:ReservedCodeCacheSize=256m",
            "-XX:+UseConcMarkSweepGC",
            "-Dversion=" + version.value
          )
      },
      // Custom release process for the plugin:
      releaseProcess := Seq[ReleaseStep](
        releaseStepCommandAndRemaining("^ publishSigned"),
        ReleaseStep(action = "sonatypeReleaseAll" :: _)
      )
    )

    lazy val crossSettings: Seq[Def.Setting[_]] = Seq(
      unmanagedSourceDirectories in Compile += {
        baseDirectory.value.getParentFile / "shared" / "src" / "main" / "scala"
      },
      unmanagedSourceDirectories in Test += {
        baseDirectory.value.getParentFile / "shared" / "src" / "test" / "scala"
      }
    )

    lazy val micrositeSettings: Seq[Def.Setting[_]] = Seq(
      micrositeName := "sbt-hood",
      micrositeBaseUrl := "/sbthood",
      micrositeDescription := "A SBT plugin for comparing benchmarks in your PRs",
      micrositeGithubOwner := "47deg",
      micrositeGithubRepo := "sbt-hood",
      micrositeGitterChannelUrl := "47deg/sbthood",
      micrositeOrganizationHomepage := "http://www.47deg.com",
      includeFilter in Jekyll := "*.html" | "*.css" | "*.png" | "*.jpg" | "*.gif" | "*.js" | "*.swf" | "*.md",
      micrositePushSiteWith := GitHub4s,
      micrositeGithubToken := sys.env.get(orgGithubTokenSetting.value),
      micrositePalette := Map(
        "brand-primary"   -> "#de3423",
        "brand-secondary" -> "#852319",
        "brand-tertiary"  -> "#381C19",
        "gray-dark"       -> "#333333",
        "gray"            -> "#666666",
        "gray-light"      -> "#EDEDED",
        "gray-lighter"    -> "#F4F5F9",
        "white-color"     -> "#E6E7EC"
      )
    )

    lazy val docsSettings: Seq[Def.Setting[_]] = Seq(
      libraryDependencies += %%("scalatest", V.scalatest),
      scalacOptions in Tut ~= (_ filterNot Set("-Ywarn-unused-import", "-Xlint").contains)
    )

  }

  import autoImport._

  override def projectSettings: Seq[Def.Setting[_]] =
    sharedReleaseProcess ++ warnUnusedImport ++ Seq(
      description := "A SBT plugin for comparing benchmarks in your PRs",
      startYear := Some(2019),
      orgProjectName := "sbt-hood",
      scalaVersion := V.scala,
      crossScalaVersions := Seq("2.11.12", V.scala),
      scalacOptions ++= scalacAdvancedOptions,
      scalacOptions ~= (_ filterNot Set("-Yliteral-types", "-Xlint").contains),
      Test / fork := true,
      Tut / scalacOptions -= "-Ywarn-unused-import",
      compileOrder in Compile := CompileOrder.JavaThenScala,
      coverageFailOnMinimum := false,
      resolvers ++= Seq(Resolver.sonatypeRepo("releases"), Resolver.sonatypeRepo("snapshots")),
      libraryDependencies ++= Seq(
        %%("scalatest", V.scalatest) % "test",
        %("slf4j-nop", V.slf4j)      % Test
      )
    ) ++ Seq(
      // sbt-org-policies settings:
      // format: OFF
      orgMaintainersSetting := List(Dev("developer47deg", Some("47 Degrees (twitter: @47deg)"), Some("hello@47deg.com"))),
      orgBadgeListSetting := List(
          GitterBadge.apply(_),
          TravisBadge.apply(_),
          CodecovBadge.apply(_),
          MavenCentralBadge.apply(_),
          LicenseBadge.apply(_),
          ScalaLangBadge.apply(_),
          GitHubIssuesBadge.apply(_)
      )
    )
  // format: ON
}
