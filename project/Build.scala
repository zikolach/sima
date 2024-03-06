import sbt.Keys._
import sbt._

object BuildSettings {
  val buildOrganization = "com.github.zikolach"
  val buildVersion      = "0.0.1"
  val buildScalaVersion = "2.13.13"

  val settings = Seq (
    organization       := buildOrganization,
    version            := buildVersion,
    scalaVersion       := buildScalaVersion,
    crossScalaVersions :=  Seq(scalaVersion.value),
    publishMavenStyle  := true,
    credentials += Credentials(Path.userHome / ".sbt" / ".credentials"),
    publishTo          := {
      val nexus = "https://oss.sonatype.org/"
      if (isSnapshot.value)
        Some("snapshots" at nexus + "content/repositories/snapshots")
      else
        Some("releases"  at nexus + "service/local/staging/deploy/maven2")
    },
    publishArtifact in Test := false,
    pomIncludeRepository := { _ => false },
    pomExtra := (
      <url>https://github.com/zikolach/sima</url>
        <licenses>
          <license>
            <name>MIT</name>
            <url>https://opensource.org/licenses/MIT</url>
            <distribution>repo</distribution>
          </license>
        </licenses>
        <scm>
          <url>git@github.com:zikolach/sima.git</url>
          <connection>scm:git:git@github.com:zikolach/sima.git</connection>
        </scm>
        <developers>
          <developer>
            <id>zikolach</id>
            <name>Nikolay Kushin</name>
            <url>http://example.com</url>
          </developer>
        </developers>)
  )
}

object Dependencies {
  val sttp = "com.softwaremill.sttp.client3" %% "core" % "3.9.3"
  val scalatest = "org.scalatest" %% "scalatest" % "3.2.17" % Test

  val miscDependencies = Seq(sttp)
  val testDependencies = Seq(scalatest)

  val allDependencies = miscDependencies ++ testDependencies
}