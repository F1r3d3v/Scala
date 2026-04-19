package com.financemanager.presentation.ui.analytics

import com.financemanager.presentation.contracts.ExpenseDataSource
import javafx.collections.{FXCollections, ListChangeListener}
import javafx.geometry.Insets
import javafx.scene.Node
import javafx.scene.chart.{BarChart, CategoryAxis, NumberAxis, PieChart, XYChart}
import javafx.scene.control.{Label, DatePicker, ComboBox}
import javafx.scene.layout.{HBox, Priority, VBox}
import java.time.temporal.{ChronoUnit, IsoFields}

import java.time.LocalDate
import scala.jdk.CollectionConverters.*

/**
 * Renders analytics charts that aggregate expenses by category and month.
 */
final class AnalyticsView(dataSource: ExpenseDataSource):
  enum TimeRange:
    case Last3Months, Last6Months, Last12Months, Custom

    override def toString: String = this match
      case Last3Months => "Last 3 Months"
      case Last6Months => "Last 6 Months"
      case Last12Months => "Last 12 Months"
      case Custom => "Custom Range"

  private val rangeSelector = new ComboBox[TimeRange](
    FXCollections.observableArrayList(TimeRange.values *)
  )
  rangeSelector.getSelectionModel.select(TimeRange.Last6Months)

  private val startDatePicker = new DatePicker(LocalDate.now().minusMonths(1))
  private val endDatePicker = new DatePicker(LocalDate.now())
  private val customDateBox = new HBox(10, new Label("From:"), startDatePicker, new Label("To:"), endDatePicker)
  customDateBox.setVisible(false)
  customDateBox.setManaged(false)

  rangeSelector.setOnAction(_ => {
    val isCustom = rangeSelector.getValue == TimeRange.Custom
    customDateBox.setVisible(isCustom)
    customDateBox.setManaged(isCustom)
    refreshCharts()
  })

  startDatePicker.setOnAction(_ => refreshCharts())
  endDatePicker.setOnAction(_ => refreshCharts())

  private val categoryPieData = FXCollections.observableArrayList[PieChart.Data]()
  private val trendSeries = new XYChart.Series[String, Number]()

  private val pieChart = new PieChart(categoryPieData)
  pieChart.setTitle("Spend by Category")
  pieChart.setLegendVisible(true)

  private val monthAxis = new CategoryAxis()
  monthAxis.setLabel("Month")
  monthAxis.setAnimated(false)

  private val amountAxis = new NumberAxis()
  amountAxis.setLabel("Amount")

  private val barChart = new BarChart[String, Number](monthAxis, amountAxis)
  barChart.setAnimated(false)
  barChart.getData.add(trendSeries)

  /** Root node rendered in the Analytics tab. */
  val root: Node =
    val container = new VBox(16)
    container.setPadding(new Insets(20))

    val title = new Label("Analytics")
    title.setStyle("-fx-font-size: 24; -fx-font-weight: bold;")

    val controls = new HBox(15, new Label("Range:"), rangeSelector, customDateBox)
    controls.setAlignment(javafx.geometry.Pos.CENTER_LEFT)

    val charts = new HBox(16, pieChart, barChart)
    HBox.setHgrow(pieChart, Priority.ALWAYS)
    HBox.setHgrow(barChart, Priority.ALWAYS)

    container.getChildren.addAll(title, controls, charts)
    container

  refreshCharts()
  dataSource.expenses.addListener(_ => refreshCharts())

  /** Refreshes all chart series from current data source values. */
  private def refreshCharts(): Unit =
    refreshCategoryPie()
    refreshMonthlyTrend()

  /** Rebuilds category totals used by the pie chart. */
  private def refreshCategoryPie(): Unit =
    val (start, end) = getDateRange
    val filteredData = dataSource.expenses.asScala.filter { e =>
      !e.date.isBefore(start) && !e.date.isAfter(end) && !e.isIncome
    }
    val perCategory = filteredData
      .groupBy(_.category)
      .view
      .mapValues(_.map(_.amount).sum)
      .toSeq
      .sortBy(entry => -entry._2.toDouble)

    categoryPieData.setAll(perCategory.map { case (category, total) =>
      new PieChart.Data(category, total.toDouble)
    }*)

  /** Rebuilds six-month spend points used by the bar chart. */
  private def refreshMonthlyTrend(): Unit =
    val (start, end) = getDateRange

    val filteredData = dataSource.expenses.asScala.filter { e =>
      !e.date.isBefore(start) && !e.date.isAfter(end)
    }

    val daysBetween = java.time.temporal.ChronoUnit.DAYS.between(start, end).toInt
    enum Granularity:
      case Daily, Weekly, Monthly

    val granularity = if daysBetween <= 8 then Granularity.Daily
                      else if daysBetween <= 42 then Granularity.Weekly
                      else Granularity.Monthly

    // Define formatting and timeline rules based on granularity
    val formatKey: LocalDate => String = granularity match
      case Granularity.Daily => d =>
        f"${d.getMonthValue}%02d-${d.getDayOfMonth}%02d"
      case Granularity.Weekly => d =>
        // e.g., "2023-W42"
        val year = d.get(IsoFields.WEEK_BASED_YEAR)
        val week = d.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)
        f"$year-W$week%02d"
      case Granularity.Monthly => d =>
        f"${d.getYear}%04d-${d.getMonthValue}%02d"

      // Generate the timeline for the X-Axis
      val timeline: Seq[LocalDate] = granularity match
        case Granularity.Daily =>
          0.to(daysBetween).map(start.plusDays(_))
        case Granularity.Weekly =>
          val weeksBetween = ChronoUnit.WEEKS.between(start, end).toInt
          0.to(weeksBetween).map(start.plusWeeks(_))
        case Granularity.Monthly =>
          val monthsBetween = ChronoUnit.MONTHS.between(start.withDayOfMonth(1), end.withDayOfMonth(1)).toInt
          0.to(monthsBetween).map(start.plusMonths(_))

    // Group data into a single Map[String, BigDecimal]
    val sums = filteredData
      .groupBy(e => formatKey(e.date))
      .view.mapValues(_.map(_.amount).sum).toMap

    // Generate points for the entire timeline, using zero for missing entries
    val points = timeline.map { date =>
      val key = formatKey(date)
      val total = sums.getOrElse(key, BigDecimal(0))
      new XYChart.Data[String, Number](key, total.toDouble)
    }

    val labelText = granularity match
      case Granularity.Daily => "Daily"
      case Granularity.Weekly => "Weekly"
      case Granularity.Monthly => "Monthly"

    monthAxis.setLabel(labelText)
    barChart.setTitle(s"Spend Trend ($labelText)")
    trendSeries.setName(s"$labelText Spend")

    trendSeries.getData.setAll(points*)

  /** Computes the date range to filter expenses based on the current range selector value. */
  private def getDateRange: (LocalDate, LocalDate) =
    val today = LocalDate.now()
    rangeSelector.getValue match
      case TimeRange.Last3Months  => (today.minusMonths(3), today)
      case TimeRange.Last6Months  => (today.minusMonths(6), today)
      case TimeRange.Last12Months => (today.minusMonths(12), today)
      case TimeRange.Custom       => (startDatePicker.getValue, endDatePicker.getValue)
