ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / scalaVersion := "3.8.3"

lazy val javafxVersion = "26"
lazy val scalaFxVersion = "26.0.0-R38"

lazy val osName = sys.props("os.name").toLowerCase
lazy val javafxPlatform =
  if (osName.contains("win")) "win"
  else if (osName.contains("mac")) "mac"
  else "linux"

lazy val root = (project in file("."))
  .settings(
    name := "FinanceManager",
    libraryDependencies ++= Seq(
      ("org.scalafx" %% "scalafx" % scalaFxVersion).cross(CrossVersion.for3Use2_13),
      "org.openjfx" % "javafx-base" % javafxVersion classifier javafxPlatform,
      "org.openjfx" % "javafx-graphics" % javafxVersion classifier javafxPlatform,
      "org.openjfx" % "javafx-controls" % javafxVersion classifier javafxPlatform,
      "org.openjfx" % "javafx-fxml" % javafxVersion classifier javafxPlatform
    ),
    Compile / run / fork := true,
    // Keeps JavaFX modules explicit when running on recent JDKs.
//    Compile / run / javaOptions ++= Seq(
//      "--add-modules",
//      "javafx.controls,javafx.fxml"
//    ),
    Compile / run / mainClass := Some("com.financemanager.app.Launcher")
  )
