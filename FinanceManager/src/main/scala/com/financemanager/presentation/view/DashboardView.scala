package com.financemanager.presentation.view

import com.financemanager.presentation.*
import com.financemanager.presentation.DisplayModels.*
import javafx.geometry.Insets
import javafx.scene.Node
import javafx.scene.control.{Label, Separator}
import javafx.scene.layout.{HBox, Priority, VBox}

/** View for the dashboard tab, responsible for rendering summary cards.
  */
final class DashboardView extends DashboardViewContract:
  private val totalSpentValue = new Label("$0.00")
  private val totalIncomeValue = new Label("$0.00")
  private val budgetRemainingValue = new Label("$0.00")
  private val transactionCountValue = new Label("0")

  private val totalSpentCard = summaryCard("Total Spent", totalSpentValue)
  private val incomeCard = summaryCard("Total Income", totalIncomeValue)
  private val budgetCard = summaryCard("Budget Remaining", budgetRemainingValue)
  private val transactionCard =
    summaryCard("Transactions This Month", transactionCountValue)

  val root: Node =
    val container = new VBox(16)
    container.setPadding(new Insets(20))

    val title = new Label("Monthly Summary")
    title.setStyle("-fx-font-size: 28; -fx-font-weight: bold;")

    val cards =
      new HBox(16, incomeCard, totalSpentCard, budgetCard, transactionCard)

    val tip = new Label(
      "Tip: Use the Transactions tab to add/edit expenses. Dashboard updates automatically."
    )

    container.getChildren.addAll(title, cards, new Separator(), tip)
    container

  override def displaySummary(summary: DashboardDisplay): Unit =
    totalIncomeValue.setText(summary.totalIncome)
    totalSpentValue.setText(summary.totalSpent)
    budgetRemainingValue.setText(summary.budgetRemaining)
    transactionCountValue.setText(summary.transactionCount.toString)

  private def summaryCard(title: String, valueLabel: Label): VBox =
    val card = new VBox(8)
    card.setPadding(new Insets(16))
    card.setStyle(
      "-fx-background-color: white; -fx-border-color: #d9d9d9; -fx-border-radius: 8; -fx-background-radius: 8;"
    )
    card.setPrefWidth(240)

    val titleLabel = new Label(title)
    titleLabel.setStyle("-fx-text-fill: #4a4a4a;")
    valueLabel.setStyle("-fx-font-size: 24; -fx-font-weight: bold;")

    HBox.setHgrow(card, Priority.ALWAYS)
    card.getChildren.addAll(titleLabel, valueLabel)
    card
