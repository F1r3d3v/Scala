ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / scalaVersion := "3.8.3"

lazy val root = (project in file("."))
  .settings(
    name := "FinanceManager",
    libraryDependencies ++= Seq(
      "org.scalafx" %% "scalafx" % "26.0.0-R38",
      "org.xerial" % "sqlite-jdbc" % "3.43.0.0",
      "com.nrinaudo" %% "kantan.csv" % "0.8.0" cross CrossVersion.for3Use2_13,
      "com.nrinaudo" %% "kantan.csv-java8" % "0.8.0" cross CrossVersion.for3Use2_13,
      "org.scalameta" %% "munit" % "1.1.1" % Test
    ),
    testFrameworks += new TestFramework("munit.Framework"),
    Compile / run / fork := true,
    Compile / run / javaOptions ++= {
      val javafxJars = (Compile / dependencyClasspath).value.files.filter(_.getName.startsWith("javafx-"))
      val sqlJars = (Compile / dependencyClasspath).value.files.filter(_.getName.startsWith("sqlite-jdbc"))
      val modulePaths = javafxJars ++ sqlJars
      Seq(
        "--enable-native-access=javafx.graphics",
        "--enable-native-access=org.xerial.sqlitejdbc",
        "--module-path", modulePaths.map(_.getAbsolutePath).mkString(java.io.File.pathSeparator),
        "--add-modules=javafx.controls"
      )
    },
    Compile / run / mainClass := Some("com.financemanager.app.FinanceManagerApp")
  )
