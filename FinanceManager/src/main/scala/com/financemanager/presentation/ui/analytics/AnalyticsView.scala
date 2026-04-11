package com.financemanager.presentation.ui.analytics

import com.financemanager.presentation.contracts.ExpenseDataSource
import javafx.collections.ListChangeListener
import javafx.geometry.Insets
import javafx.scene.Node
import javafx.scene.chart.{BarChart, CategoryAxis, NumberAxis, PieChart, XYChart}
import javafx.scene.control.Label
import javafx.scene.layout.{HBox, Priority, VBox}

import java.time.LocalDate
import scala.jdk.CollectionConverters.*

final class AnalyticsView(dataSource: ExpenseDataSource):
  private val categoryPieData = javafx.collections.FXCollections.observableArrayList[PieChart.Data]()
  private val monthlySeries = new XYChart.Series[String, Number]()
  monthlySeries.setName("Monthly Spend")

  private val pieChart = new PieChart(categoryPieData)
  pieChart.setTitle("Spend by Category")
  pieChart.setLegendVisible(true)

  private val monthAxis = new CategoryAxis()
  monthAxis.setLabel("Month")

  private val amountAxis = new NumberAxis()
  amountAxis.setLabel("Amount")

  private val barChart = new BarChart[String, Number](monthAxis, amountAxis)
  barChart.setTitle("Spend Trend (Last 6 Months)")
  barChart.getData.add(monthlySeries)

  val root: Node =
    val container = new VBox(16)
    container.setPadding(new Insets(20))

    val title = new Label("Analytics")
    title.setStyle("-fx-font-size: 24; -fx-font-weight: bold;")

    val charts = new HBox(16, pieChart, barChart)
    HBox.setHgrow(pieChart, Priority.ALWAYS)
    HBox.setHgrow(barChart, Priority.ALWAYS)

    container.getChildren.addAll(title, charts)
    container

  refreshCharts()
  dataSource.expenses.addListener((_: ListChangeListener.Change[? <: com.financemanager.model.Expense]) => refreshCharts())

  private def refreshCharts(): Unit =
    refreshCategoryPie()
    refreshMonthlyTrend()

  private def refreshCategoryPie(): Unit =
    val perCategory = dataSource.expenses.asScala
      .groupBy(_.category)
      .view
      .mapValues(_.map(_.amount).sum)
      .toSeq
      .sortBy(entry => -entry._2.toDouble)

    categoryPieData.setAll(perCategory.map { case (category, total) =>
      new PieChart.Data(category, total.toDouble)
    }*)

  private def refreshMonthlyTrend(): Unit =
    val today = LocalDate.now()
    val lastSix = 0.to(5).reverse.map(today.minusMonths(_))

    val points = lastSix.map { date =>
      val monthKey = f"${date.getYear}%04d-${date.getMonthValue}%02d"
      val total = dataSource.expenses.asScala
        .filter(e => e.date.getYear == date.getYear && e.date.getMonthValue == date.getMonthValue)
        .map(_.amount)
        .sum
      new XYChart.Data[String, Number](monthKey, total.toDouble)
    }

    monthlySeries.getData.setAll(points*)
