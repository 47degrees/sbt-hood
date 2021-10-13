import com.typesafe.sbt.site.jekyll.JekyllPlugin.autoImport._
import microsites.MicrositeKeys._
import sbt.Keys._
import sbt._

import scala.language.reflectiveCalls

object ProjectPlugin extends AutoPlugin {

  override def trigger: PluginTrigger = allRequirements

  object autoImport {

    lazy val micrositeSettings: Seq[Def.Setting[_]] = Seq(
      micrositeName                 := "SBT-Hood",
      micrositeBaseUrl              := "/sbt-hood",
      micrositeDescription          := "A SBT plugin for comparing benchmarks in your PRs",
      micrositeDocumentationUrl     := "docs",
      micrositeGitterChannelUrl     := "47deg/sbthood",
      micrositeOrganizationHomepage := "http://www.47deg.com",
      includeFilter in Jekyll := "*.html" | "*.css" | "*.png" | "*.jpg" | "*.gif" | "*.js" | "*.swf" | "*.md" | "*.svg",
      micrositePushSiteWith   := GitHub4s,
      micrositeHighlightTheme := "atom-one-light",
      micrositeGithubToken    := Option(System.getenv().get("GITHUB_TOKEN")),
      micrositePalette := Map(
        "brand-primary"   -> "#25bc77",
        "brand-secondary" -> "#25bc77",
        "white-color"     -> "#FFF"
      )
    )

  }

  override def projectSettings: Seq[Def.Setting[_]] =
    Seq(
      resolvers += Resolver.typesafeIvyRepo("releases"),
      libraryDependencies ++= Seq(
        "io.circe"       %% "circe-generic"       % "0.14.1",
        "io.circe"       %% "circe-core"          % "0.14.1",
        "io.circe"       %% "circe-parser"        % "0.14.1",
        "org.http4s"     %% "http4s-blaze-client" % "0.21.31",
        "com.47deg"      %% "github4s"            % "0.28.5",
        "org.typelevel"  %% "cats-effect"         % "2.5.4",
        "org.typelevel"  %% "log4cats-slf4j"      % "1.3.1",
        "ch.qos.logback"  % "logback-classic"     % "1.2.6",
        "com.nrinaudo"   %% "kantan.csv"          % "0.6.2",
        "com.nrinaudo"   %% "kantan.csv-generic"  % "0.6.2",
        "dev.profunktor" %% "console4cats"        % "0.8.1",
        "com.lightbend"  %% "emoji"               % "1.2.3",
        "org.scalatest"  %% "scalatest"           % "3.2.10" % Test,
        "org.slf4j"       % "slf4j-nop"           % "1.7.32" % Test
      )
    )
}
