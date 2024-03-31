ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.13"

lazy val root = (project in file("."))
  .settings(
    name := "sima"
  )
  .settings(BuildSettings.settings *)
  .settings(libraryDependencies ++= Dependencies.rootDependencies)
  .settings(
    scalacOptions ++= Seq(
      "-Ymacro-annotations",
      "-unchecked",
      "-deprecation",
      "-Xfatal-warnings",
//      "-Xlint",
      "-feature"
    )
  )

lazy val integrationTest = (project in file("it"))
  .settings(BuildSettings.commonSettings *)
  .settings(
    publish / skip := true,
    libraryDependencies ++= Dependencies.itDependencies
  )
  .dependsOn(root)


lazy val demo = (project in file("demo"))
  .settings(BuildSettings.settings *)
  .settings(libraryDependencies ++= Dependencies.demoDependencies)
  .settings(
    scalacOptions ++= Seq(
      "-Ymacro-annotations",
      "-unchecked",
      "-deprecation",
      "-Xfatal-warnings",
      //      "-Xlint",
      "-feature"
    )
  )
  .dependsOn(root)

releasePublishArtifactsAction := PgpKeys.publishSigned.key
