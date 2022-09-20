ThisBuild / scalaVersion     := "2.13.9"
ThisBuild / version          := "0.1.0-SNAPSHOT"
ThisBuild / organization     := "com.krishna"

lazy val root = (project in file("."))
  .settings(
    name := "zio-learning",
    libraryDependencies ++= Seq(
      "dev.zio" %% "zio" % "2.0.2",
      "dev.zio" %% "zio-streams" % "2.0.2",
      "dev.zio" %% "zio-json" % "0.3.0",
      "dev.zio" %% "zio-test" % "2.0.2" % Test
    ),
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")
  )
