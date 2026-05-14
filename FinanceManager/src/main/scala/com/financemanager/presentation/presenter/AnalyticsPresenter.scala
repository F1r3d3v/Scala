package com.financemanager.presentation.presenter

import com.financemanager.domain.repository.TransactionRepository
import com.financemanager.domain.service.AnalyticsService
import com.financemanager.presentation.*
import com.financemanager.presentation.DisplayModels.*

import java.time.LocalDate

/**
 * Presenter that prepares analytics data for charts.
 *
 * @param view analytics UI contract
 * @param analyticsService domain service for analytics aggregation
 * @param repository repository to observe for updates
 */
final class AnalyticsPresenter(
    view: AnalyticsViewContract,
    analyticsService: AnalyticsService,
    repository: TransactionRepository
) extends AnalyticsPresenterContract:

  private var currentRange: TimeRangeSelection = TimeRangeSelection.Last6Months

  override def onViewCreated(): Unit =
    refresh()
    repository.subscribe(() => refresh())

  override def onRangeChanged(range: TimeRangeSelection): Unit =
    currentRange = range
    refresh()

  private def refresh(): Unit =
    val (start, end) = dateRange
    val categoryData = analyticsService.spendingByCategory()
      .map((cat, amount) => cat -> amount.toDouble)
    view.displayCategoryBreakdown(categoryData)

    val trend = analyticsService.spendingTrend(start, end)
    view.displaySpendingTrend(TrendData(trend.points, trend.periodLabel))

  private def dateRange: (LocalDate, LocalDate) =
    val today = LocalDate.now()
    currentRange match
      case TimeRangeSelection.Last3Months  => (today.minusMonths(3), today)
      case TimeRangeSelection.Last6Months  => (today.minusMonths(6), today)
      case TimeRangeSelection.Last12Months => (today.minusMonths(12), today)
      case TimeRangeSelection.Custom(s, e) => (s, e)
