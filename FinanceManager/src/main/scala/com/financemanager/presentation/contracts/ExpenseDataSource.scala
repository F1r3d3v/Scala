package com.financemanager.presentation.contracts

import com.financemanager.model.{Transaction, TransactionInput}
import javafx.collections.ObservableList

/** Exposes the observable list of expenses used by UI components. */
trait ExpenseDataSource:
  /** Returns live expense data that can be bound to JavaFX controls. */
  def expenses: ObservableList[Transaction]

/** Defines expense mutations triggered from the transactions UI. */
trait ExpenseCommands:
  /** Lists categories available in the category selector. */
  def categories: Seq[String]

  /**
   * Adds a new expense after validating the provided input.
   *
   * @return validation error message or success marker
   */
  def addExpense(input: TransactionInput): Either[String, Unit]

  /**
   * Updates an existing expense by id using validated input.
   *
   * @return validation or lookup error message, or success marker
   */
  def updateExpense(id: Long, input: TransactionInput): Either[String, Unit]

  /** Deletes an expense by id if it exists. */
  def deleteExpense(id: Long): Unit

/** Provides budget values used by dashboard summaries. */
trait BudgetProvider:
  /** Returns the configured monthly budget used to compute remaining funds. */
  def monthlyBudget: BigDecimal

