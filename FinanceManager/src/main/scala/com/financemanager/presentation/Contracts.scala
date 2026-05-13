package com.financemanager.presentation

import com.financemanager.domain.model.{Category, TransactionId, TransactionInput, TransactionType}

import java.time.LocalDate

/**
 * UI-specific models used to decouple presenters from views.
 */
object DisplayModels:

  /**
   * Display-ready transaction row.
   */
  final case class TransactionDisplay(
      id: TransactionId,
      date: String,
      amount: String,
      rawAmount: BigDecimal,
      category: String,
      description: String,
      transactionType: TransactionType
  )

  /**
   * Display-ready dashboard summary.
   */
  final case class DashboardDisplay(
      totalSpent: String,
      totalIncome: String,
      budgetRemaining: String,
      transactionCount: Int
  )

  /**
   * Chart-ready spend trend data.
   *
   * @param points ordered list of (label, value) pairs
   * @param periodLabel label for the time bucket
   */
  final case class TrendData(points: Seq[(String, Double)], periodLabel: String):
    def chartTitle: String = s"Spend Trend ($periodLabel)"
    def seriesName: String = s"$periodLabel Spend"

  /**
   * Predefined time-range options for analytics.
   */
  enum TimeRangeSelection:
    case Last3Months, Last6Months, Last12Months
    case Custom(start: LocalDate, end: LocalDate)

import com.financemanager.presentation.DisplayModels.*

/**
 * Contract implemented by the transactions view.
 */
trait TransactionsViewContract:
  def displayTransactions(transactions: Seq[TransactionDisplay]): Unit
  def displayCategories(categories: Seq[Category]): Unit
  def displayError(message: String): Unit
  def resetForm(): Unit
  def populateForm(transaction: TransactionDisplay): Unit

/**
 * Contract implemented by the dashboard view.
 */
trait DashboardViewContract:
  def displaySummary(summary: DashboardDisplay): Unit

/**
 * Contract implemented by the analytics view.
 */
trait AnalyticsViewContract:
  def displayCategoryBreakdown(data: Seq[(String, Double)]): Unit
  def displaySpendingTrend(data: TrendData): Unit

/**
 * Presenter actions initiated by the transactions view.
 */
trait TransactionsPresenterContract:
  def onViewCreated(): Unit
  def onSubmit(input: TransactionInput): Unit
  def onDelete(id: TransactionId): Unit
  def onTransactionSelected(id: TransactionId): Unit
  def onClearSelection(): Unit

/**
 * Presenter actions initiated by the dashboard view.
 */
trait DashboardPresenterContract:
  def onViewCreated(): Unit

/**
 * Presenter actions initiated by the analytics view.
 */
trait AnalyticsPresenterContract:
  def onViewCreated(): Unit
  def onRangeChanged(range: TimeRangeSelection): Unit
