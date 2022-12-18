name        := "zio-learning"
description := "Sample learning code using Scala and ZIO functional library"

ThisBuild / version      := "0.1.0-SNAPSHOT"
ThisBuild / organization := "com.krishna"

lazy val root = (project in file(".")).aggregate(zioSample, zioRockTheJvm)

lazy val zioVersion                        = "2.0.5"
lazy val zioCommonLibraries: Seq[ModuleID] = Seq(
  "dev.zio" %% "zio"         % zioVersion,
  "dev.zio" %% "zio-streams" % zioVersion,
  "dev.zio" %% "zio-test"    % zioVersion % Test
)

lazy val zioSample = project
  .settings(
    name         := "zioSample",
    scalaVersion := "2.13.9",
    libraryDependencies ++= Seq(
      "dev.zio" %% "zio-kafka" % "2.0.1",
      "dev.zio" %% "zio-json"  % "0.3.0",
      "io.d11"  %% "zhttp"     % "2.0.0-RC11"
    ) ++ zioCommonLibraries,
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")
  )

lazy val zioRockTheJvm = project
  .settings(
    name         := "zioRockTheJvm",
    scalaVersion := "3.2.1",
    libraryDependencies ++= Seq(
      "dev.zio" %% "zio-test-sbt"   % zioVersion,
      "dev.zio" %% "zio-test-junit" % zioVersion
    ) ++ zioCommonLibraries,
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")
  )
