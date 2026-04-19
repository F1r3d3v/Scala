package com.financemanager.presentation.mock

import com.financemanager.model.{Transaction, TransactionInput}
import com.financemanager.presentation.contracts.{BudgetProvider, ExpenseCommands, ExpenseDataSource}
import javafx.collections.{FXCollections, ObservableList}

import java.time.LocalDate
import scala.jdk.CollectionConverters.*

/**
 * In-memory implementation of expense contracts used by the demo UI.
 */
final class InMemoryExpenseController extends ExpenseDataSource, ExpenseCommands, BudgetProvider:
  private var nextId: Long = 5L

  override val monthlyBudget: BigDecimal = BigDecimal(0)

  override val expenses: ObservableList[Transaction] = FXCollections.observableArrayList(
    Transaction(1L, LocalDate.now().minusDays(2), BigDecimal(79.90), "Groceries", "Weekly grocery shopping", false),
    Transaction(2L, LocalDate.now().minusDays(5), BigDecimal(45.00), "Transport", "Fuel refill", false),
    Transaction(3L, LocalDate.now().minusDays(7), BigDecimal(22.50), "Subscriptions", "Cloud storage", false),
    Transaction(4L, LocalDate.now().minusDays(10), BigDecimal(130.00), "Utilities", "Electricity bill", false),
    Transaction(5L, LocalDate.now().minusDays(1), BigDecimal(2500.00), "Salary", "Monthly Salary", true),
    Transaction(6L, LocalDate.now().minusDays(3), BigDecimal(150.00), "Freelance", "Web design task", true)
  )
  
  override val categories: Seq[String] = Seq(
    "Groceries", "Transport", "Utilities", "Dining", "Subscriptions", "Health", "Other",
    "Salary", "Freelance", "Investments"
  )

  /** Adds a new expense when input validation succeeds. */
  override def addExpense(input: TransactionInput): Either[String, Unit] =
    validate(input).map { _ =>
      expenses.add(Transaction(nextId, input.date, input.amount, input.category, input.description, input.isIncome))
      nextId += 1
    }

  /** Updates an existing expense by id when validation and lookup succeed. */
  override def updateExpense(id: Long, input: TransactionInput): Either[String, Unit] =
    validate(input).flatMap { _ =>
      expenses.asScala.indexWhere(_.id == id) match
        case -1 => Left(s"Expense with id=$id not found")
        case idx =>
          expenses.set(idx, Transaction(id, input.date, input.amount, input.category, input.description, input.isIncome))
          Right(())
    }

  /** Removes an expense by id; no-op when id is not found. */
  override def deleteExpense(id: Long): Unit =
    expenses.asScala.indexWhere(_.id == id) match
      case -1 => ()
      case idx => expenses.remove(idx)

  /** Performs basic business validation shared by add and update operations. */
  private def validate(input: TransactionInput): Either[String, Unit] =
    if input.amount <= 0 then Left("Amount must be greater than zero")
    else if input.category.trim.isEmpty then Left("Category is required")
    else if input.description.trim.isEmpty then Left("Description is required")
    else Right(())

