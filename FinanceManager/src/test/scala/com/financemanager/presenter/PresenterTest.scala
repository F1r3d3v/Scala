package com.financemanager.presenter

import com.financemanager.domain.model.{Category, Transaction, TransactionId, TransactionInput, TransactionType, CategoryId}
import com.financemanager.domain.repository.InMemoryCategoryRepository
import com.financemanager.domain.service.{AnalyticsService, BudgetService, CategoryMaintenanceService, CategoryService, ImportExportService, TransactionService}
import com.financemanager.presentation.DisplayModels.TimeRangeSelection
import com.financemanager.presentation.presenter.{AnalyticsPresenter, DashboardPresenter, TransactionsPresenter}
import com.financemanager.testutil.{MockAnalyticsView, MockDashboardView, MockTransactionsView, TestRepository}

import java.time.LocalDate
import munit.FunSuite

/**
 * Tests for transactions presenter behavior.
 */
class TransactionsPresenterTest extends FunSuite:

  private val now = LocalDate.now()
  private def sampleTransaction(id: Long, amount: BigDecimal, tType: TransactionType) =
    Transaction(TransactionId(id), now, amount, CategoryId(1L), "Lunch", tType)

  test("onViewCreated displays categories and transactions"):
    val repo = TestRepository(sampleTransaction(1, BigDecimal("50"), TransactionType.Expense))
    val view = MockTransactionsView()
    val service = TransactionService(repo)
    val categoryService = new CategoryService(new InMemoryCategoryRepository(Seq(Category(CategoryId(1L), "Food"), Category(CategoryId(2L), "Transport"))))
    val categoryMaintenanceService = new CategoryMaintenanceService(categoryService, repo)
    val presenter = TransactionsPresenter(
      view,
      service,
      categoryService,
      categoryMaintenanceService,
      repo,
      new ImportExportService(repo, service, categoryService)
    )

    presenter.onViewCreated()

    assert(view.displayedCategories.nonEmpty, clue = "expected categories to be loaded")
    assertEquals(view.displayedTransactions.size, 1, clue = "expected one transaction to be displayed")
    assertEquals(view.displayedTransactions.head.rawAmount, BigDecimal("50"), clue = "expected the seeded amount to be shown")

  test("onSubmit with valid input adds transaction"):
    val repo = TestRepository()
    val view = MockTransactionsView()
    val service = TransactionService(repo)
    val categoryService = new CategoryService(new InMemoryCategoryRepository())
    val categoryMaintenanceService = new CategoryMaintenanceService(categoryService, repo)
    val presenter = TransactionsPresenter(
      view,
      service,
      categoryService,
      categoryMaintenanceService,
      repo,
      new ImportExportService(repo, service, categoryService)
    )
    presenter.onViewCreated()

    val input = TransactionInput(now, BigDecimal("50"), CategoryId(1L), "Lunch", TransactionType.Expense)
    presenter.onSubmit(input)

    assertEquals(view.formResets, 1, clue = "expected form to reset after a successful add")
    assertEquals(view.displayedTransactions.size, 1, clue = "expected the new transaction to appear")

  test("onSubmit with invalid input shows error"):
    val repo = TestRepository()
    val view = MockTransactionsView()
    val service = TransactionService(repo)
    val categoryService = new CategoryService(new InMemoryCategoryRepository())
    val categoryMaintenanceService = new CategoryMaintenanceService(categoryService, repo)
    val presenter = TransactionsPresenter(
      view,
      service,
      categoryService,
      categoryMaintenanceService,
      repo,
      new ImportExportService(repo, service, categoryService)
    )
    presenter.onViewCreated()

    val input = TransactionInput(now, BigDecimal(0), CategoryId(1L), "Lunch", TransactionType.Expense)
    presenter.onSubmit(input)

    assertEquals(view.errors.size, 1, clue = "expected a validation error to be shown")
    assertEquals(view.formResets, 0, clue = "expected the form to remain unchanged on error")

  test("onTransactionSelected populates form"):
    val t = sampleTransaction(1, BigDecimal("50"), TransactionType.Expense)
    val repo = TestRepository(t)
    val view = MockTransactionsView()
    val ts = TransactionService(repo)
    val categoryService = new CategoryService(new InMemoryCategoryRepository())
    val categoryMaintenanceService = new CategoryMaintenanceService(categoryService, repo)
    val presenter = TransactionsPresenter(
      view,
      ts,
      categoryService,
      categoryMaintenanceService,
      repo,
      new ImportExportService(repo, ts, categoryService)
    )
    presenter.onViewCreated()

    presenter.onTransactionSelected(TransactionId(1L))

    assertEquals(view.populatedForms.size, 1, clue = "expected the selected transaction to populate the form")
    assertEquals(view.populatedForms.head.id, TransactionId(1L), clue = "expected the selected id to match")

  test("onDelete removes transaction and resets form"):
    val t = sampleTransaction(1, BigDecimal("50"), TransactionType.Expense)
    val repo = TestRepository(t)
    val view = MockTransactionsView()
    val ts = TransactionService(repo)
    val categoryService = new CategoryService(new InMemoryCategoryRepository())
    val categoryMaintenanceService = new CategoryMaintenanceService(categoryService, repo)
    val presenter = TransactionsPresenter(
      view,
      ts,
      categoryService,
      categoryMaintenanceService,
      repo,
      new ImportExportService(repo, ts, categoryService)
    )
    presenter.onViewCreated()

    presenter.onDelete(TransactionId(1L))

    assertEquals(view.formResets, 1, clue = "expected the form to reset after delete")
    assertEquals(view.displayedTransactions.size, 0, clue = "expected the transaction list to be empty after delete")

  test("onDelete non-existent shows error"):
    val repo = TestRepository()
    val view = MockTransactionsView()
    val ts = TransactionService(repo)
    val categoryService = new CategoryService(new InMemoryCategoryRepository())
    val categoryMaintenanceService = new CategoryMaintenanceService(categoryService, repo)
    val presenter = TransactionsPresenter(
      view,
      ts,
      categoryService,
      categoryMaintenanceService,
      repo,
      new ImportExportService(repo, ts, categoryService)
    )
    presenter.onViewCreated()

    presenter.onDelete(TransactionId(999L))

    assertEquals(view.errors.size, 1, clue = "expected an error for a missing transaction")

  test("onClearSelection resets form"):
    val repo = TestRepository()
    val view = MockTransactionsView()
    val ts = TransactionService(repo)
    val categoryService = new CategoryService(new InMemoryCategoryRepository())
    val categoryMaintenanceService = new CategoryMaintenanceService(categoryService, repo)
    val presenter = TransactionsPresenter(
      view,
      ts,
      categoryService,
      categoryMaintenanceService,
      repo,
      new ImportExportService(repo, ts, categoryService)
    )
    presenter.onViewCreated()

    presenter.onClearSelection()

    assertEquals(view.formResets, 1, clue = "expected clear selection to reset the form")

  test("onSubmit after selection performs update"):
    val t = sampleTransaction(1, BigDecimal("50"), TransactionType.Expense)
    val repo = TestRepository(t)
    val view = MockTransactionsView()
    val ts = TransactionService(repo)
    val categoryService = new CategoryService(new InMemoryCategoryRepository())
    val categoryMaintenanceService = new CategoryMaintenanceService(categoryService, repo)
    val presenter = TransactionsPresenter(
      view,
      ts,
      categoryService,
      categoryMaintenanceService,
      repo,
      new ImportExportService(repo, ts, categoryService)
    )
    presenter.onViewCreated()

    presenter.onTransactionSelected(TransactionId(1L))
    val input = TransactionInput(now, BigDecimal("75"), CategoryId(1L), "Updated lunch", TransactionType.Expense)
    presenter.onSubmit(input)

    assertEquals(view.formResets, 1, clue = "expected the form to reset after update")
    assertEquals(view.displayedTransactions.head.rawAmount, BigDecimal("75"), clue = "expected the updated amount to be shown")

  test("onAddCategory refreshes displayed categories"):
    val repo = TestRepository()
    val view = MockTransactionsView()
    val transactionService = TransactionService(repo)
    val categoryService = new CategoryService(new InMemoryCategoryRepository(Seq(Category(CategoryId(1L), "Food"))))
    val categoryMaintenanceService = new CategoryMaintenanceService(categoryService, repo)
    val presenter = TransactionsPresenter(
      view,
      transactionService,
      categoryService,
      categoryMaintenanceService,
      repo,
      new ImportExportService(repo, transactionService, categoryService)
    )
    presenter.onViewCreated()

    presenter.onAddCategory("Travel")

    assert(view.displayedCategories.exists(_.name == "Travel"), clue = "expected new category to be displayed")
    assert(view.displayedCategoriesHistory.size >= 2, clue = "expected categories to refresh after add")

  test("onDeleteCategory removes category from displayed list"):
    val repo = TestRepository()
    val view = MockTransactionsView()
    val transactionService = TransactionService(repo)
    val categoryService = new CategoryService(new InMemoryCategoryRepository(Seq(Category(CategoryId(1L), "Food"), Category(CategoryId(2L), "Travel"))))
    val categoryMaintenanceService = new CategoryMaintenanceService(categoryService, repo)
    val presenter = TransactionsPresenter(
      view,
      transactionService,
      categoryService,
      categoryMaintenanceService,
      repo,
      new ImportExportService(repo, transactionService, categoryService)
    )
    presenter.onViewCreated()

    presenter.onDeleteCategory(CategoryId(2L))

    assert(!view.displayedCategories.exists(_.name == "Travel"), clue = "expected deleted category to disappear")

  test("deleting category with assigned transactions asks for confirmation and reassigns to Unknown"):
    val repo = TestRepository(
      Transaction(TransactionId(1L), now, BigDecimal("50"), CategoryId(2L), "Lunch", TransactionType.Expense)
    )
    val view = MockTransactionsView()
    val transactionService = TransactionService(repo)
    val categoryService = new CategoryService(new InMemoryCategoryRepository(Seq(Category(CategoryId(2L), "Food"))))
    val categoryMaintenanceService = new CategoryMaintenanceService(categoryService, repo)
    val presenter = TransactionsPresenter(
      view,
      transactionService,
      categoryService,
      categoryMaintenanceService,
      repo,
      new ImportExportService(repo, transactionService, categoryService)
    )
    presenter.onViewCreated()

    presenter.onDeleteCategory(CategoryId(2L))

    assertEquals(view.deletionConfirmations.head.assignedTransactionCount, 1)
    assertEquals(view.displayedTransactions.head.category, "Unknown")
    assert(view.displayedCategories.exists(_.name == "Unknown"))

/**
 * Tests for dashboard presenter summary rendering.
 */
class DashboardPresenterTest extends FunSuite:

  private val now = LocalDate.now()

  test("onViewCreated displays summary"):
    val transactions = Seq(
      Transaction(TransactionId(1), now, BigDecimal("100"), CategoryId(1L), "Lunch", TransactionType.Expense),
      Transaction(TransactionId(2), now, BigDecimal("500"), CategoryId(2L), "Pay", TransactionType.Income)
    )
    val repo = TestRepository(transactions*)
    val view = MockDashboardView()
    val presenter = DashboardPresenter(view, new BudgetService(repo), repo)

    presenter.onViewCreated()

    assert(view.lastSummary.isDefined, clue = "expected a dashboard summary")
    val summary = view.lastSummary.get
    assertEquals(summary.transactionCount, 2, clue = "expected two transactions in the summary")
    assert(summary.totalSpent.contains("100"), clue = "expected spent amount to include 100")
    assert(summary.totalIncome.contains("500"), clue = "expected income amount to include 500")

/**
 * Tests for analytics presenter chart data.
 */
class AnalyticsPresenterTest extends FunSuite:

  private val now = LocalDate.now()

  test("onViewCreated displays charts"):
    val transactions = Seq(
      Transaction(TransactionId(1), now, BigDecimal("50"), CategoryId(1L), "Lunch", TransactionType.Expense),
      Transaction(TransactionId(2), now.minusMonths(1), BigDecimal("30"), CategoryId(2L), "Bus", TransactionType.Expense)
    )
    val repo = TestRepository(transactions*)
    val view = MockAnalyticsView()
    val categoryRepo = new InMemoryCategoryRepository(Seq(Category(CategoryId(1L), "Food"), Category(CategoryId(2L), "Transport")))
    val service = new AnalyticsService(repo, categoryRepo)
    val presenter = AnalyticsPresenter(view, service, repo)

    presenter.onViewCreated()

    assert(view.categoryData.nonEmpty, clue = "expected category breakdown data")
    assert(view.trendData.isDefined, clue = "expected spending trend data")

  test("onRangeChanged updates data"):
    val transactions = Seq(
      Transaction(TransactionId(1), now, BigDecimal("50"), CategoryId(1L), "Lunch", TransactionType.Expense)
    )
    val repo = TestRepository(transactions*)
    val view = MockAnalyticsView()
    val categoryRepo = new InMemoryCategoryRepository(Seq(Category(CategoryId(1L), "Food")))
    val service = new AnalyticsService(repo, categoryRepo)
    val presenter = AnalyticsPresenter(view, service, repo)
    presenter.onViewCreated()

    presenter.onRangeChanged(TimeRangeSelection.Last3Months)

    assert(view.categoryData.nonEmpty, clue = "expected category data to remain populated after range change")

  test("onRangeChanged filters data by range"):
    val oldDate = now.minusMonths(10)
    val transactions = Seq(
      Transaction(TransactionId(1), now, BigDecimal("50"), CategoryId(1L), "Recent", TransactionType.Expense),
      Transaction(TransactionId(2), oldDate, BigDecimal("100"), CategoryId(1L), "Old", TransactionType.Expense)
    )
    val repo = TestRepository(transactions*)
    val view = MockAnalyticsView()
    val categoryRepo = new InMemoryCategoryRepository(Seq(Category(CategoryId(1L), "Food")))
    val service = new AnalyticsService(repo, categoryRepo)
    val presenter = AnalyticsPresenter(view, service, repo)

    // Default range is Last 6 Months
    presenter.onViewCreated()
    assertEquals(view.categoryData.head._2, 50.0, clue = "expected only recent transaction in default 6M range")

    // Change to Last 12 Months
    presenter.onRangeChanged(TimeRangeSelection.Last12Months)
    assertEquals(view.categoryData.head._2, 150.0, clue = "expected both transactions in 12M range")

    // Change to custom range excluding the old one
    presenter.onRangeChanged(TimeRangeSelection.Custom(now.minusDays(1), now.plusDays(1)))
    assertEquals(view.categoryData.head._2, 50.0, clue = "expected only recent transaction in custom range")
