package com.financemanager.presentation.view

import com.financemanager.presentation.*
import com.financemanager.presentation.DisplayModels.*
import javafx.collections.FXCollections
import javafx.geometry.Insets
import javafx.scene.Node
import javafx.scene.chart.{
  BarChart,
  CategoryAxis,
  NumberAxis,
  PieChart,
  XYChart
}
import javafx.scene.control.{ComboBox, DatePicker, Label}
import javafx.scene.layout.{HBox, Priority, VBox}

import java.time.LocalDate

import scala.compiletime.uninitialized

/** View for the analytics tab, rendering category breakdown and trend charts.
  */
final class AnalyticsView extends AnalyticsViewContract:
  /** Presenter assigned by the application during wiring. */
  var presenter: AnalyticsPresenterContract = uninitialized

  private enum DisplayRange:
    case Last3Months, Last6Months, Last12Months, Custom
    override def toString: String = this match
      case Last3Months  => "Last 3 Months"
      case Last6Months  => "Last 6 Months"
      case Last12Months => "Last 12 Months"
      case Custom       => "Custom Range"

  private val rangeSelector = new ComboBox[DisplayRange](
    FXCollections.observableArrayList(DisplayRange.values*)
  )
  rangeSelector.getSelectionModel.select(DisplayRange.Last6Months)

  private val startDatePicker = new DatePicker(LocalDate.now().minusMonths(1))
  private val endDatePicker = new DatePicker(LocalDate.now())
  private val customDateBox = new HBox(
    10,
    new Label("From:"),
    startDatePicker,
    new Label("To:"),
    endDatePicker
  )
  customDateBox.setVisible(false)
  customDateBox.setManaged(false)

  private val categoryPieData =
    FXCollections.observableArrayList[PieChart.Data]()
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

  val root: Node =
    val container = new VBox(16)
    container.setPadding(new Insets(20))

    val title = new Label("Analytics")
    title.setStyle("-fx-font-size: 24; -fx-font-weight: bold;")

    val controls =
      new HBox(15, new Label("Range:"), rangeSelector, customDateBox)
    controls.setAlignment(javafx.geometry.Pos.CENTER_LEFT)

    val charts = new HBox(16, pieChart, barChart)
    HBox.setHgrow(pieChart, Priority.ALWAYS)
    HBox.setHgrow(barChart, Priority.ALWAYS)

    container.getChildren.addAll(title, controls, charts)
    container

  rangeSelector.setOnAction(_ => {
    val isCustom = rangeSelector.getValue == DisplayRange.Custom
    customDateBox.setVisible(isCustom)
    customDateBox.setManaged(isCustom)
    notifyPresenter()
  })

  startDatePicker.setOnAction(_ => notifyPresenter())
  endDatePicker.setOnAction(_ => notifyPresenter())

  override def displayCategoryBreakdown(data: Seq[(String, Double)]): Unit =
    categoryPieData.setAll(data.map { case (category, total) =>
      new PieChart.Data(category, total)
    }*)

  override def displaySpendingTrend(data: TrendData): Unit =
    monthAxis.setLabel(data.periodLabel)
    barChart.setTitle(data.chartTitle)
    trendSeries.setName(data.seriesName)
    trendSeries.getData.setAll(data.points.map { case (key, value) =>
      new XYChart.Data[String, Number](key, value)
    }*)

  private def notifyPresenter(): Unit =
    val selection = rangeSelector.getValue match
      case DisplayRange.Last3Months  => TimeRangeSelection.Last3Months
      case DisplayRange.Last6Months  => TimeRangeSelection.Last6Months
      case DisplayRange.Last12Months => TimeRangeSelection.Last12Months
      case DisplayRange.Custom       =>
        TimeRangeSelection.Custom(
          startDatePicker.getValue,
          endDatePicker.getValue
        )
    presenter.onRangeChanged(selection)
