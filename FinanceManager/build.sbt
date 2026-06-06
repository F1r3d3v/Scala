ThisBuild / version := "0.0.0"
ThisBuild / scalaVersion := "3.8.3"

lazy val mainClassName  = "com.financemanager.app.FinanceManagerApp"
lazy val jpackageAppImage = taskKey[Unit]("Prepare JARs and run jpackage to produce app-image")

lazy val nativeAccessOpts = Seq(
  "--enable-native-access=javafx.graphics",
  "--enable-native-access=org.xerial.sqlitejdbc",
  "--add-reads=org.xerial.sqlitejdbc=ALL-UNNAMED"
)

lazy val root = (project in file("."))
  .enablePlugins(JavaAppPackaging)
  .settings(
    name := "FinanceManager",
    libraryDependencies ++= Seq(
      "org.openjfx" % "javafx-controls" % "26.0.1",
      "com.typesafe.slick" %% "slick" % "3.6.1",
      "org.slf4j" % "slf4j-nop" % "2.0.18",
      "com.nrinaudo" %% "kantan.csv" % "0.8.0" cross CrossVersion.for3Use2_13,
      "com.nrinaudo" %% "kantan.csv-java8" % "0.8.0" cross CrossVersion.for3Use2_13,
      "org.xerial" % "sqlite-jdbc" % "3.53.2.0",
      "org.scalameta" %% "munit" % "1.3.2" % Test
    ),
    testFrameworks += new TestFramework("munit.Framework"),
    Compile / run / fork := true,
    Compile / run / mainClass := Some(mainClassName),
    Compile / mainClass       := Some(mainClassName),
    Compile / run / javaOptions ++= {
      val javafxJars = (Compile / dependencyClasspath).value.files.filter(_.getName.startsWith("javafx-"))
      val sqlJars = (Compile / dependencyClasspath).value.files.filter(_.getName.startsWith("sqlite-jdbc"))
      val modulePaths = javafxJars ++ sqlJars
      nativeAccessOpts ++ Seq(
        "--module-path", modulePaths.map(_.getAbsolutePath).mkString(java.io.File.pathSeparator),
        "--add-modules=javafx.controls"
      )
    },
    Universal / javaOptions ++= nativeAccessOpts,
    jpackageAppImage := {
      import scala.sys.process._
      import java.nio.file.{Files, StandardCopyOption}

      val log      = streams.value.log
      val baseDir  = (ThisBuild / baseDirectory).value
      val libDir   = baseDir / "target" / "universal" / "stage" / "lib"
      val jmodsDir = baseDir / "target" / "universal" / "stage" / "modules"
      val destDir  = baseDir / "dist" / "raw"

      IO.delete(jmodsDir)
      IO.createDirectory(jmodsDir)
      IO.delete(destDir)
      IO.createDirectory(destDir)

      val javaHome = file(System.getProperty("java.home"))
      val jpackage = (javaHome / "bin" / "jpackage").getAbsolutePath
      log.info(s"Using jpackage: $jpackage")

      val normalizedName = name.value.toLowerCase
      val mainJar = IO.listFiles(libDir)
        .find(_.getName.matches(s"$normalizedName\\.$normalizedName-.*\\.jar"))
        .getOrElse(sys.error(s"Application JAR not found in $libDir"))
      log.info(s"Main JAR: ${mainJar.getName}")

      val osClassifier = (System.getProperty("os.name").toLowerCase, System.getProperty("os.arch").toLowerCase) match {
        case (s, _)    if s.contains("win")                              => "win"
        case (s, arch) if s.contains("mac") && arch.contains("aarch64") => "mac-aarch64"
        case (s, _)    if s.contains("mac")                              => "mac"
        case (_, arch) if arch.contains("aarch64")                       => "linux-aarch64"
        case _                                                           => "linux"
      }

      val nativeModules = (Universal / javaOptions).value
        .collect { case s if s.startsWith("--enable-native-access=") => s.stripPrefix("--enable-native-access=") }
        .filterNot(_.startsWith("javafx."))
        .toSet
      def norm(s: String) = s.replaceAll("[^a-zA-Z0-9]", "").toLowerCase
      val versionSuffix = """(.+)-\d.*\.jar""".r

      IO.listFiles(libDir).foreach { f =>
        if (f.getName.startsWith("org.openjfx.") && f.getName.contains(s"-$osClassifier.")) {
          Files.move(f.toPath, (jmodsDir / f.getName).toPath, StandardCopyOption.REPLACE_EXISTING)
          log.info(s"Moved JavaFX module: ${f.getName}")
        } else {
          val jarBase = f.getName match { case versionSuffix(base) => norm(base); case _ => "" }
          if (jarBase.nonEmpty && nativeModules.exists(m => norm(m) == jarBase)) {
            Files.copy(f.toPath, (jmodsDir / f.getName).toPath, StandardCopyOption.REPLACE_EXISTING)
            log.info(s"Copied native module: ${f.getName}")
          }
        }
      }

      val jfxModules = libraryDependencies.value
        .filter(_.organization == "org.openjfx")
        .map(_.name.replace("-", "."))
        .mkString(",")

      val allModules = (jfxModules.split(",").toSeq ++ nativeModules.toSeq).mkString(",")
      log.info(s"All modules: $allModules")

      val javaOptArgs = (Universal / javaOptions).value.flatMap(Seq("--java-options", _))

      val cmd = Seq(
        jpackage,
        "--type",          "app-image",
        "--name",          name.value,
        "--input",         libDir.getAbsolutePath,
        "--main-jar",      mainJar.getName,
        "--main-class",    (Compile / mainClass).value.getOrElse(sys.error("mainClass not set")),
        "--module-path",   jmodsDir.getAbsolutePath,
        "--add-modules",   allModules,
        "--jlink-options", "--strip-debug --compress=zip-6 --no-header-files --no-man-pages",
        "--dest",          destDir.getAbsolutePath,
      ) ++ javaOptArgs

      log.info(s"Running: ${cmd.mkString(" ")}")
      val exit = cmd.!
      if (exit != 0) sys.error(s"jpackage failed with exit code $exit")
    },
    jpackageAppImage := (jpackageAppImage dependsOn (Universal / stage)).value,

    Test / fork := true,
    Test / javaOptions ++= Seq(
      "--enable-native-access=org.xerial.sqlitejdbc",
      "--add-reads=org.xerial.sqlitejdbc=ALL-UNNAMED"
    )
  )
