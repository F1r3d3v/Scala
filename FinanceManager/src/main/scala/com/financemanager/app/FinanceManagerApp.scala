package com.financemanager.app

import com.financemanager.presentation.mock.InMemoryExpenseController
import com.financemanager.presentation.ui.analytics.AnalyticsView
import com.financemanager.presentation.ui.dashboard.DashboardView
import com.financemanager.presentation.ui.main.MainView
import com.financemanager.presentation.ui.transactions.TransactionsView
import javafx.application.Application
import javafx.scene.Scene
import javafx.stage.Stage

/** JavaFX application entry that wires controllers and top-level views. */
final class FinanceManagerApp extends Application:
  /** Initializes and shows the primary stage.
   * @param primaryStage the main application window provided by JavaFX runtime
   * */
  override def start(primaryStage: Stage): Unit =
    val expenseController = new InMemoryExpenseController

    val dashboardView = new DashboardView(expenseController, expenseController)
    val transactionsView = new TransactionsView(expenseController, expenseController)
    val analyticsView = new AnalyticsView(expenseController)
    val mainView = new MainView(dashboardView.root, transactionsView.root, analyticsView.root)

    val scene = new Scene(mainView.root, 1180, 760)
    primaryStage.setTitle("Personal Finance Manager")
    primaryStage.setMinWidth(980)
    primaryStage.setMinHeight(640)
    primaryStage.setScene(scene)
    primaryStage.show()

/** JVM-friendly launcher object for the JavaFX application. */
object FinanceManagerApp:
  /** Delegates startup to JavaFX runtime. */
  def main(args: Array[String]): Unit =
    Application.launch(classOf[FinanceManagerApp], args*)
