ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / scalaVersion := "3.8.3"
lazy val scalaFxVersion = "26.0.0-R38"

lazy val root = (project in file("."))
  .settings(
    name := "FinanceManager",
    libraryDependencies ++= Seq(
      "org.scalafx" %% "scalafx" % scalaFxVersion
    ),
    Compile / run / fork := true,
    Compile / run / javaOptions ++= {
      val javafxJars = (Compile / dependencyClasspath).value.files.filter(_.getName.startsWith("javafx-"))
      Seq(
        "--enable-native-access=javafx.graphics",
        "--module-path", javafxJars.map(_.getAbsolutePath).mkString(java.io.File.pathSeparator),
        "--add-modules=javafx.controls"
      )
    },
    Compile / run / mainClass := Some("com.financemanager.app.Launcher")
  )
