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
      Jekyll / includeFilter := "*.html" | "*.css" | "*.png" | "*.jpg" | "*.gif" | "*.js" | "*.swf" | "*.md" | "*.svg",
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
        "io.circe"              %% "circe-generic"       % "0.14.5",
        "io.circe"              %% "circe-core"          % "0.14.5",
        "io.circe"              %% "circe-parser"        % "0.14.5",
        "org.http4s"            %% "http4s-blaze-client" % "0.23.14",
        "com.47deg"             %% "github4s"            % "0.32.0",
        "org.typelevel"         %% "cats-effect"         % "3.4.8",
        "org.typelevel"         %% "log4cats-slf4j"      % "2.5.0",
        "ch.qos.logback"         % "logback-classic"     % "1.4.6",
        "com.nrinaudo"          %% "kantan.csv"          % "0.7.0",
        "com.nrinaudo"          %% "kantan.csv-generic"  % "0.7.0",
        "com.lightbend"         %% "emoji"               % "1.3.0",
        "com.github.marklister" %% "base64"              % "0.3.0",
        "org.scalatest"         %% "scalatest"           % "3.2.15" % Test,
        "org.typelevel"         %% "log4cats-noop"       % "2.5.0"  % Test
      )
    )
}
