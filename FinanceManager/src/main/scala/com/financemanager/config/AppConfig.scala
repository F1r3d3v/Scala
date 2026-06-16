package com.financemanager.config

import java.nio.file.{Path, Paths}

object AppConfig:
  val AppName = "Personal Finance Manager"

  // Dimensions
  val WindowWidth = 1180
  val WindowHeight = 760
  val MinWindowWidth = 980
  val MinWindowHeight = 640

  // Database
  val DbDirectoryName = ".financemanager"
  val DbFileName = "finance.db"

  def getDatabasePath: Path =
    // Keeping data in User Home so that `sbt clean` doesn't delete user's financial data.
    Paths.get(System.getProperty("user.home"), DbDirectoryName, DbFileName)

  val InitialCategories: Seq[String] = Seq(
    "Groceries",
    "Transport",
    "Utilities",
    "Dining",
    "Subscriptions",
    "Health",
    "Other",
    "Salary",
    "Freelance",
    "Investments"
  )
