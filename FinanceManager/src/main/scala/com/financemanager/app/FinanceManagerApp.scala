package com.financemanager.app

import com.financemanager.presentation.mock.InMemoryExpenseController
import com.financemanager.presentation.ui.analytics.AnalyticsView
import com.financemanager.presentation.ui.dashboard.DashboardView
import com.financemanager.presentation.ui.main.MainView
import com.financemanager.presentation.ui.transactions.TransactionsView
import javafx.application.Application
import javafx.scene.Scene
import javafx.stage.Stage

final class FinanceManagerApp extends Application:
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

object FinanceManagerApp:
  def main(args: Array[String]): Unit =
    Application.launch(classOf[FinanceManagerApp], args*)
