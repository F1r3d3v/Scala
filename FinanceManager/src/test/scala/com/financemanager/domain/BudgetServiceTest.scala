package com.financemanager.domain

import com.financemanager.domain.model.{Transaction, TransactionId, TransactionType, CategoryId}
import com.financemanager.domain.service.BudgetService
import com.financemanager.testutil.TestRepository
import java.time.LocalDate
import munit.FunSuite

/**
 * Tests for budget summary calculations.
 */
class BudgetServiceTest extends FunSuite:

  private val now = LocalDate.now()

  private def expense(amount: BigDecimal, id: Long = 1) =
    Transaction(TransactionId(id), now, amount, CategoryId(1L), "Desc", TransactionType.Expense)

  private def income(amount: BigDecimal, id: Long = 1) =
    Transaction(TransactionId(id), now, amount, CategoryId(1L), "Desc", TransactionType.Income)

  test("computeSummary with mixed transactions returns correct totals"):
    val transactions = Seq(expense(BigDecimal("50")), expense(BigDecimal("30")), income(BigDecimal("200")))
    val service = new BudgetService(TestRepository(transactions*))
    val summary = service.computeSummary()
    assertEquals(summary.totalSpent, BigDecimal("80"), clue = "expected total spent from expenses")
    assertEquals(summary.totalIncome, BigDecimal("200"), clue = "expected total income from income transactions")
    assertEquals(summary.remaining, BigDecimal("120"), clue = "expected remaining as income minus expenses")
    assertEquals(summary.transactionCount, 3, clue = "expected all transactions to be counted")

  test("computeSummary with only expenses has zero income"):
    val transactions = Seq(expense(BigDecimal("100")), expense(BigDecimal("50")))
    val service = new BudgetService(TestRepository(transactions*))
    val summary = service.computeSummary()
    assertEquals(summary.totalIncome, BigDecimal(0), clue = "expected zero income when there are no income transactions")
    assertEquals(summary.totalSpent, BigDecimal("150"), clue = "expected expenses to be summed")

  test("computeSummary with only income has zero spent"):
    val transactions = Seq(income(BigDecimal("500")))
    val service = new BudgetService(TestRepository(transactions*))
    val summary = service.computeSummary()
    assertEquals(summary.totalSpent, BigDecimal(0), clue = "expected zero spent when there are no expenses")
    assertEquals(summary.totalIncome, BigDecimal("500"), clue = "expected income to be summed")

  test("computeSummary with empty list returns zeros"):
    val service = new BudgetService(TestRepository())
    val summary = service.computeSummary()
    assertEquals(summary.totalSpent, BigDecimal(0), clue = "expected zero spent for empty repository")
    assertEquals(summary.totalIncome, BigDecimal(0), clue = "expected zero income for empty repository")
    assertEquals(summary.remaining, BigDecimal(0), clue = "expected zero remaining for empty repository")
    assertEquals(summary.transactionCount, 0, clue = "expected zero transactions for empty repository")

  test("computeSummary can have negative remaining"):
    val transactions = Seq(expense(BigDecimal("500")), income(BigDecimal("200")))
    val service = new BudgetService(TestRepository(transactions*))
    val summary = service.computeSummary()
    assertEquals(summary.remaining, BigDecimal("-300"), clue = "expected negative remaining when expenses exceed income")
