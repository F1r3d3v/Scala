package com.financemanager.domain.service

import com.financemanager.domain.model.Extensions.*
import com.financemanager.domain.repository.TransactionRepository
import java.time.YearMonth

/** Aggregated budget metrics for the dashboard.
  *
  * @param totalSpent
  *   summed expenses
  * @param totalIncome
  *   summed income
  * @param remaining
  *   income minus expenses
  * @param transactionCount
  *   total number of transactions in scope
  */
final case class BudgetSummary(
    totalSpent: BigDecimal,
    totalIncome: BigDecimal,
    remaining: BigDecimal,
    transactionCount: Int
)

/** Domain service that computes budget summaries from transactions.
  *
  * @param repository
  *   source of persisted transactions
  */
final class BudgetService(repository: TransactionRepository):
  def computeSummary(): BudgetSummary =
    val transactions = repository.findAll().filterByMonth(YearMonth.now())
    val totalSpent = transactions.expensesOnly.totalAmount
    val totalIncome = transactions.incomesOnly.totalAmount
    val remaining = totalIncome - totalSpent
    BudgetSummary(totalSpent, totalIncome, remaining, transactions.size)
