package com.financemanager.presentation.ui.dashboard

import com.financemanager.presentation.contracts.{BudgetProvider, ExpenseDataSource}
import javafx.collections.ListChangeListener
import javafx.geometry.Insets
import javafx.scene.Node
import javafx.scene.control.{Label, Separator}
import javafx.scene.layout.{HBox, Priority, VBox}

import java.time.LocalDate
import scala.jdk.CollectionConverters.*

/**
 * Displays monthly summary cards based on current expenses and configured budget.
 */
final class DashboardView(dataSource: ExpenseDataSource, budgetProvider: BudgetProvider):
  private val totalSpentValue = new Label("$0.00")
  private val budgetRemainingValue = new Label("$0.00")
  private val transactionCountValue = new Label("0")

  private val totalSpentCard = summaryCard("Total Spent", totalSpentValue)
  private val budgetCard = summaryCard("Budget Remaining", budgetRemainingValue)
  private val transactionCard = summaryCard("Transactions This Month", transactionCountValue)

  /** Root node rendered in the Dashboard tab. */
  val root: Node =
    val container = new VBox(16)
    container.setPadding(new Insets(20))

    val title = new Label("Monthly Summary")
    title.setStyle("-fx-font-size: 28; -fx-font-weight: bold;")

    val cards = new HBox(16, totalSpentCard, budgetCard, transactionCard)

    val tip = new Label("Tip: Use the Transactions tab to add/edit expenses. Dashboard updates automatically.")

    container.getChildren.addAll(title, cards, new Separator(), tip)
    container

  updateSummary()
  dataSource.expenses.addListener((_: ListChangeListener.Change[? <: com.financemanager.model.Expense]) => updateSummary())

  /** Recomputes all summary values for the current month. */
  private def updateSummary(): Unit =
    val now = LocalDate.now()
    val monthlyExpenses = dataSource.expenses.asScala.filter { expense =>
      expense.date.getMonthValue == now.getMonthValue && expense.date.getYear == now.getYear
    }

    val totalSpent = monthlyExpenses.map(_.amount).sum
    val budgetLeft = budgetProvider.monthlyBudget - totalSpent

    totalSpentValue.setText("$" + f"${totalSpent.toDouble}%.2f")
    budgetRemainingValue.setText("$" + f"${budgetLeft.toDouble}%.2f")
    transactionCountValue.setText(monthlyExpenses.size.toString)

  /** Creates a styled summary card used by dashboard metrics. */
  private def summaryCard(title: String, valueLabel: Label): VBox =
    val card = new VBox(8)
    card.setPadding(new Insets(16))
    card.setStyle("-fx-background-color: white; -fx-border-color: #d9d9d9; -fx-border-radius: 8; -fx-background-radius: 8;")
    card.setPrefWidth(240)

    val titleLabel = new Label(title)
    titleLabel.setStyle("-fx-text-fill: #4a4a4a;")

    valueLabel.setStyle("-fx-font-size: 24; -fx-font-weight: bold;")

    HBox.setHgrow(card, Priority.ALWAYS)
    card.getChildren.addAll(titleLabel, valueLabel)
    card
