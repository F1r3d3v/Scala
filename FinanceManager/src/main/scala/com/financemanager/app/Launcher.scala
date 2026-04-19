package com.financemanager.app

/** Small compatibility launcher delegating to the main JavaFX application object. */
object Launcher {
  /** Forwards command-line arguments to the main app launcher. */
  def main(args: Array[String]): Unit = {
    FinanceManagerApp.main(args)
  }
}