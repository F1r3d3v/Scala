package com.financemanager.presentation.contracts

import com.financemanager.model.{Expense, ExpenseInput}
import javafx.collections.ObservableList

trait ExpenseDataSource:
  def expenses: ObservableList[Expense]

trait ExpenseCommands:
  def categories: Seq[String]
  def addExpense(input: ExpenseInput): Either[String, Unit]
  def updateExpense(id: Long, input: ExpenseInput): Either[String, Unit]
  def deleteExpense(id: Long): Unit

trait BudgetProvider:
  def monthlyBudget: BigDecimal

