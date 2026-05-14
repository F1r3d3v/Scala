package com.financemanager.domain.service

import com.financemanager.domain.model.CategoryId
import com.financemanager.domain.model.Extensions.*
import com.financemanager.domain.repository.{CategoryRepository, TransactionRepository}

import java.time.LocalDate
import java.time.temporal.{ChronoUnit, IsoFields}

/**
 * Time-bucketing options used when aggregating trend data.
 */
enum Granularity:
  case Daily, Weekly, Monthly

/**
 * Aggregated trend data with labels suitable for charting.
 *
 * @param points ordered list of (label, value) pairs
 * @param granularity time bucket used when computing the series
 */
final case class TrendResult(points: Seq[(String, Double)], granularity: Granularity):
  def periodLabel: String = granularity match
    case Granularity.Daily   => "Daily"
    case Granularity.Weekly  => "Weekly"
    case Granularity.Monthly => "Monthly"

/**
 * Domain service for category breakdowns and spend trends.
 *
 * @param repository source of persisted transactions
 * @param categoryRepository source of categories for display names
 */
final class AnalyticsService(repository: TransactionRepository, categoryRepository: CategoryRepository):
  def spendingByCategory(): Seq[(String, BigDecimal)] =
    val transactions = repository.findAll()
    val categories = categoryRepository.findAll()
    val nameById = categories.map(c => c.id -> c.name).toMap

    transactions.expensesOnly
      .groupBy(_.category)
      .view.mapValues(_.totalAmount)
      .toSeq
      .map { case (catId, amount) => nameById.getOrElse(catId, s"Unknown") -> amount }
      .sortBy(-_._2)

  def spendingTrend(start: LocalDate, end: LocalDate): TrendResult =
    val transactions = repository.findAll()
    val daysBetween = ChronoUnit.DAYS.between(start, end).toInt
    val granularity = if daysBetween <= 8 then Granularity.Daily
                      else if daysBetween <= 42 then Granularity.Weekly
                      else Granularity.Monthly

    val formatKey: LocalDate => String = granularity match
      case Granularity.Daily => d => f"${d.getMonthValue}%02d-${d.getDayOfMonth}%02d"
      case Granularity.Weekly => d =>
        f"${d.get(IsoFields.WEEK_BASED_YEAR)}-W${d.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)}%02d"
      case Granularity.Monthly => d => f"${d.getYear}%04d-${d.getMonthValue}%02d"

    val timeline: Seq[LocalDate] = granularity match
      case Granularity.Daily =>
        (0 to daysBetween).map(start.plusDays(_))
      case Granularity.Weekly =>
        (0 to ChronoUnit.WEEKS.between(start, end).toInt).map(start.plusWeeks(_))
      case Granularity.Monthly =>
        val months = ChronoUnit.MONTHS.between(start.withDayOfMonth(1), end.withDayOfMonth(1)).toInt
        (0 to months).map(start.plusMonths(_))

    val sums = transactions.expensesOnly
      .filter(t => !t.date.isBefore(start) && !t.date.isAfter(end))
      .groupBy(t => formatKey(t.date))
      .view.mapValues(_.map(_.amount).sum).toMap

    val points = timeline.map { date =>
      val key = formatKey(date)
      key -> sums.getOrElse(key, BigDecimal(0)).toDouble
    }

    TrendResult(points, granularity)
