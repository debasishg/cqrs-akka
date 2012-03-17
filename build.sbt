name := "cqrs-akka"

organization := "net.debasishg"

version := "0.1"

crossScalaVersions := Seq("2.9.1", "2.9.0", "2.8.1", "2.8.0")

// resolvers ++= Seq("Twitter Repository" at "http://maven.twttr.com", "Akka Repository" at "http://akka.io/repository")

resolvers ++= Seq("Akka Repository" at "http://repo.typesafe.com/typesafe/releases/")

libraryDependencies <++= scalaVersion { scalaVersion =>
  // Helper for dynamic version switching based on scalaVersion
  val scalatestVersion: String => String = Map(("2.8.0" -> "1.3.1.RC2"), ("2.8.1" -> "1.5.1")) getOrElse (_, "1.6.1")
  // The dependencies with proper scope
  Seq(
    "org.scala-lang.plugins"         % "continuations"   % scalaVersion,
    "net.debasishg"                 %% "redisclient"     % "2.4.2",
    "net.debasishg"                 %% "sjson"           % "0.15",
    "org.slf4j"                      % "slf4j-api"       % "1.6.1",
    "org.slf4j"                      % "slf4j-log4j12"   % "1.6.1"            % "provided",
    "log4j"                          % "log4j"           % "1.2.16"           % "provided",
    "junit"                          % "junit"           % "4.8.1"            % "test",
    "org.scalatest"                 %% "scalatest"       % "1.6.1"            % "test",
    "org.scalaz"                    %% "scalaz-core"     % "6.0.3",
    "com.typesafe.akka"              % "akka-actor"      % "2.0",
    "com.typesafe.akka"              % "akka-testkit"    % "2.0",
    "ch.qos.logback"                 % "logback-classic" % "0.9.28"           % "runtime"
  )
}

autoCompilerPlugins := true

addCompilerPlugin("org.scala-lang.plugins" % "continuations" % "2.9.1")

scalacOptions ++= Seq("-encoding", "UTF-8", "-deprecation", "-Xcheckinit", "-P:continuations:enable")

// TODO: Enable this with SBT 0.10.2 (See: https://github.com/harrah/xsbt/issues/147)
// scaladocOptions <++= (name, version) map { (name, ver) =>
//  Seq("-doc-title", name, "-doc-version", ver)
//}

parallelExecution in Test := false

publishTo := Some("Scala-Tools Nexus Repository for Releases" at "http://nexus.scala-tools.org/content/repositories/releases")

credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")
