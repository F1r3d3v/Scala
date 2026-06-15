package com.financemanager.app

import com.financemanager.domain.service.{AnalyticsService, BudgetService, CategoryMaintenanceService, CategoryService, ImportExportService, TransactionService}
import com.financemanager.infrastructure.slick.{SlickDatabaseManager, SlickCategoryRepository, SlickTransactionRepository}
import com.financemanager.presentation.presenter.{AnalyticsPresenter, DashboardPresenter, TransactionsPresenter}
import com.financemanager.presentation.view.{AnalyticsView, DashboardView, MainView, TransactionsView}
import com.financemanager.config.AppConfig
import javafx.application.Application
import javafx.scene.Scene
import javafx.stage.Stage
import java.nio.file.Files

/**
 * JavaFX entry point that wires repositories, services, presenters, and views.
 */
final class FinanceManagerApp extends Application:
  override def start(primaryStage: Stage): Unit =
    val dbPath = AppConfig.getDatabasePath
    Files.createDirectories(dbPath.getParent)
    val dbManager = new SlickDatabaseManager(s"jdbc:sqlite:${dbPath.toAbsolutePath}")
    dbManager.initializeSchema()

    val categoryRepository = new SlickCategoryRepository(dbManager)
    val repository = new SlickTransactionRepository(dbManager)

    val transactionService = new TransactionService(repository)
    val budgetService = new BudgetService(repository)
    val analyticsService = new AnalyticsService(repository, categoryRepository)
    val categoryService = new CategoryService(categoryRepository)
    val categoryMaintenanceService = new CategoryMaintenanceService(categoryService, repository)
    val importExportService = new ImportExportService(repository, transactionService, categoryService)

    val dashboardView = new DashboardView()
    val transactionsView = new TransactionsView()
    val analyticsView = new AnalyticsView()

    val dashboardPresenter = DashboardPresenter(dashboardView, budgetService, repository)
    val transactionsPresenter = TransactionsPresenter(transactionsView, transactionService, categoryService, categoryMaintenanceService, repository, importExportService)
    val analyticsPresenter = AnalyticsPresenter(analyticsView, analyticsService, repository)

    transactionsView.presenter = transactionsPresenter
    analyticsView.presenter = analyticsPresenter

    dashboardPresenter.onViewCreated()
    transactionsPresenter.onViewCreated()
    analyticsPresenter.onViewCreated()

    val mainView = new MainView(dashboardView.root, transactionsView.root, analyticsView.root)

    val scene = new Scene(mainView.root, AppConfig.WindowWidth, AppConfig.WindowHeight)
    primaryStage.setTitle(AppConfig.AppName)
    primaryStage.setMinWidth(AppConfig.MinWindowWidth)
    primaryStage.setMinHeight(AppConfig.MinWindowHeight)
    primaryStage.setScene(scene)
    primaryStage.show()

/**
 * JVM entry point for launching the JavaFX application.
 */
object FinanceManagerApp:
  def main(args: Array[String]): Unit =
    Application.launch(classOf[FinanceManagerApp], args*)
