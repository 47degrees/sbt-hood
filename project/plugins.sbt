resolvers += Resolver.sonatypeRepo("releases")
resolvers += Resolver.sonatypeRepo("snapshots")

addSbtPlugin("com.47deg"          % "sbt-org-policies" % "0.13.3")
addSbtPlugin("com.47deg"          % "sbt-microsites"   % "1.1.5")
addSbtPlugin("com.eed3si9n"       % "sbt-buildinfo"    % "0.9.0")
addSbtPlugin("com.geirsson"       % "sbt-ci-release"   % "1.5.2")
addSbtPlugin("pl.project13.scala" % "sbt-jmh"          % "0.3.7")
addSbtPlugin("com.timushev.sbt"   % "sbt-updates"      % "0.5.0")
addSbtPlugin("org.scalameta"      % "sbt-scalafmt"     % "2.3.3")
addSbtPlugin("org.scoverage"      % "sbt-scoverage"    % "1.6.1")

libraryDependencies += "org.scala-sbt" %% "scripted-plugin" % sbtVersion.value
