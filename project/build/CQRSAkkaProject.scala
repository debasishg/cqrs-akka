import sbt._

class CQRSAkkaProject(info: ProjectInfo) extends DefaultProject(info) with AkkaBaseProject 
{
  val scalaToolsSnapshots = "Scala-Tools Maven2 Snapshot Repository" at "http://scala-tools.org/repo-snapshots"
  val scalaToolsReleases = "Scala-Tools Maven2 Releases Repository" at "http://scala-tools.org/repo-releases"
  val embeddedRepo       = "Embedded Repo" at (info.projectPath / "embedded-repo").asURL.toString
  val akkaRepo = "Akka Repository" at "http://akka.io/repository"

  val scalazDep = "org.scalaz" %% "scalaz-core" % "6.0.1"
  val akkaActor = "se.scalablesolutions.akka" % "akka-actor"  % "1.2-RC2"

  val scalatest = "org.scalatest" % "scalatest_2.9.0-1" % "1.6.1" % "test"

  val junit = "junit" % "junit" % "4.8.1"

  override def packageSrcJar = defaultJarPath("-sources.jar")
  lazy val sourceArtifact = Artifact.sources(artifactID)
  override def packageToPublishActions = super.packageToPublishActions ++ Seq(packageSrc)

  override def managedStyle = ManagedStyle.Maven
  Credentials(Path.userHome / ".ivy2" / ".credentials", log)
  lazy val publishTo = "Scala Tools Nexus" at "http://nexus.scala-tools.org/content/repositories/releases/"
//  lazy val publishTo = Resolver.file("Local Test Repository", Path fileProperty "java.io.tmpdir" asFile)
}
