//noinspection scala2InSource3
import sbt.Keys._
//noinspection scala2InSource3
import sbt._

//noinspection TypeAnnotation,ScalaUnnecessaryParentheses
object BuildSettings {
  val buildOrganization = "com.github.zikolach"
  val buildVersion = "0.0.1"
  val buildScalaVersion = "2.13.13"

  val commonSettings = Seq(
    organization := buildOrganization,
    version := buildVersion,
    scalaVersion := buildScalaVersion,
    crossScalaVersions := Seq(scalaVersion.value),
  )

  val settings = commonSettings ++ Seq(
    publishMavenStyle := true,
    credentials += Credentials(Path.userHome / ".sbt" / ".credentials"),
    publishTo := {
      val nexus = "https://oss.sonatype.org/"
      if (isSnapshot.value)
        Some("snapshots" at nexus + "content/repositories/snapshots")
      else
        Some("releases" at nexus + "service/local/staging/deploy/maven2")
    },
    Test / publishArtifact := false,
    pomIncludeRepository := { _ =>
      false
    },
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
      </developers>
    )
  )
}

//noinspection TypeAnnotation
object Dependencies {
  val catsEffect = "org.typelevel" %% "cats-effect" % "3.5.3"
  private val sttpVersion = "3.9.3"

  val sttp = "com.softwaremill.sttp.client3" %% "core" % sttpVersion
  val sttpCE = "com.softwaremill.sttp.client3" %% "fs2" % sttpVersion

  private val circeVersion = "0.14.6"
  val circeCore = "io.circe" %% "circe-core" % circeVersion
  val circeGeneric = "io.circe" %% "circe-generic" % circeVersion
  val circeParser = "io.circe" %% "circe-parser" % circeVersion

  val scalatest = "org.scalatest" %% "scalatest" % "3.2.17" % Test
  val catsScalaTest = "org.typelevel" %% "cats-effect-testing-scalatest" % "1.5.0" % Test
  val testContainers = "com.dimafeng" %% "testcontainers-scala-scalatest" % "0.41.2" % Test

  val testDependencies = Seq(scalatest, catsScalaTest)

  val PekkoVersion = "1.0.2"
  val PekkoHttpVersion = "1.0.1"
  val pekkoActor = "org.apache.pekko" %% "pekko-actor" % PekkoVersion
  val pekkoActorTyped = "org.apache.pekko" %% "pekko-actor-typed" % PekkoVersion
  val pekkoStream = "org.apache.pekko" %% "pekko-stream" % PekkoVersion
  val pekkoHttp = "org.apache.pekko" %% "pekko-http" % PekkoHttpVersion
  val scalatags = "com.lihaoyi" %% "scalatags" % "0.12.0"
  val slf4jSimple = "org.slf4j" % "slf4j-simple" % "2.0.12"

  val rootDependencies = Seq(catsEffect, sttp, sttpCE, circeCore, circeGeneric, circeParser)
  val demoDependencies = Seq(pekkoActor, pekkoActorTyped, pekkoStream, pekkoHttp, scalatags, slf4jSimple)
  val itDependencies = testDependencies :+ testContainers
}
