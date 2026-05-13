package com.financemanager.app

import com.financemanager.domain.model.{Category, Transaction, TransactionId, TransactionType, CategoryId}
import com.financemanager.domain.repository.{InMemoryCategoryRepository, InMemoryTransactionRepository, TransactionRepository}
import com.financemanager.domain.service.{AnalyticsService, BudgetService, CategoryService, TransactionService}
import com.financemanager.presentation.presenter.{AnalyticsPresenter, DashboardPresenter, TransactionsPresenter}
import com.financemanager.presentation.view.{AnalyticsView, DashboardView, MainView, TransactionsView}
import javafx.application.Application
import javafx.scene.Scene
import javafx.stage.Stage
import java.time.LocalDate

/**
 * JavaFX entry point that wires repositories, services, presenters, and views.
 */
final class FinanceManagerApp extends Application:
  override def start(primaryStage: Stage): Unit =
    val repository = seededRepository()
    val categoryRepository = new InMemoryCategoryRepository(Seq(
      Category(CategoryId(1L), "Groceries"),
      Category(CategoryId(2L), "Transport"),
      Category(CategoryId(3L), "Utilities"),
      Category(CategoryId(4L), "Dining"),
      Category(CategoryId(5L), "Subscriptions"),
      Category(CategoryId(6L), "Health"),
      Category(CategoryId(7L), "Other"),
      Category(CategoryId(8L), "Salary"),
      Category(CategoryId(9L), "Freelance"),
      Category(CategoryId(10L), "Investments")
    ))

    val transactionService = new TransactionService(repository)
    val budgetService = new BudgetService(repository)
    val analyticsService = new AnalyticsService(repository, categoryRepository)
    val categoryService = new CategoryService(categoryRepository)

    val dashboardView = new DashboardView()
    val transactionsView = new TransactionsView()
    val analyticsView = new AnalyticsView()

    val dashboardPresenter = DashboardPresenter(dashboardView, budgetService, repository)
    val transactionsPresenter = TransactionsPresenter(transactionsView, transactionService, categoryService, repository)
    val analyticsPresenter = AnalyticsPresenter(analyticsView, analyticsService, repository)

    transactionsView.presenter = transactionsPresenter
    analyticsView.presenter = analyticsPresenter

    dashboardPresenter.onViewCreated()
    transactionsPresenter.onViewCreated()
    analyticsPresenter.onViewCreated()

    val mainView = new MainView(dashboardView.root, transactionsView.root, analyticsView.root)

    val scene = new Scene(mainView.root, 1180, 760)
    primaryStage.setTitle("Personal Finance Manager")
    primaryStage.setMinWidth(980)
    primaryStage.setMinHeight(640)
    primaryStage.setScene(scene)
    primaryStage.show()

  /**
   * Creates an in-memory repository pre-seeded with sample transactions.
   */
  private def seededRepository(): TransactionRepository =
    val now = LocalDate.now()
    val initial = Seq(
      Transaction(TransactionId(1), now.minusDays(2), BigDecimal("79.90"), CategoryId(1L), "Weekly grocery shopping", TransactionType.Expense),
      Transaction(TransactionId(2), now.minusDays(5), BigDecimal("45.00"), CategoryId(2L), "Fuel refill", TransactionType.Expense),
      Transaction(TransactionId(3), now.minusDays(7), BigDecimal("22.50"), CategoryId(5L), "Cloud storage", TransactionType.Expense),
      Transaction(TransactionId(4), now.minusDays(10), BigDecimal("130.00"), CategoryId(3L), "Electricity bill", TransactionType.Expense),
      Transaction(TransactionId(5), now.minusDays(1), BigDecimal("2500.00"), CategoryId(8L), "Monthly Salary", TransactionType.Income),
      Transaction(TransactionId(6), now.minusDays(3), BigDecimal("150.00"), CategoryId(9L), "Web design task", TransactionType.Income)
    )
    new InMemoryTransactionRepository(initial)

/**
 * JVM entry point for launching the JavaFX application.
 */
object FinanceManagerApp:
  def main(args: Array[String]): Unit =
    Application.launch(classOf[FinanceManagerApp], args*)
