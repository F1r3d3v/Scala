package com.financemanager.domain.model

import java.time.{LocalDate, YearMonth}

/**
 * Extension helpers used throughout the domain and presentation layers.
 */
object Extensions:

  extension (bd: BigDecimal)
    /**
     * Formats a currency value with two decimal places.
     */
    def moneyFormat: String = "$" + f"${bd.toDouble}%.2f"

  /**
   * Collection helpers for filtering and aggregating transactions.
   */
  extension (transactions: Seq[Transaction])
    def filterByMonth(ym: YearMonth): Seq[Transaction] =
      transactions.filter(t => YearMonth.from(t.date) == ym)

    def filterByRange(start: LocalDate, end: LocalDate): Seq[Transaction] =
      transactions.filter(t => !t.date.isBefore(start) && !t.date.isAfter(end))

    def expensesOnly: Seq[Transaction] =
      transactions.filter(_.transactionType == TransactionType.Expense)

    def incomesOnly: Seq[Transaction] =
      transactions.filter(_.transactionType == TransactionType.Income)

    def totalAmount: BigDecimal = transactions.map(_.amount).sum
