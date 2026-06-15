package com.financemanager.testutil

import com.financemanager.domain.model.Category
import com.financemanager.domain.service.CategoryDeletionPreview
import com.financemanager.presentation.*
import com.financemanager.presentation.DisplayModels.*
import scala.collection.mutable.ListBuffer

/**
 * Test double for the transactions view that records calls for assertions.
 */
final class MockTransactionsView extends TransactionsViewContract:
  var displayedTransactions: Seq[TransactionDisplay] = Seq.empty
  var displayedCategories: Seq[Category] = Seq.empty
  var errors: ListBuffer[String] = ListBuffer.empty
  var formResets: Int = 0
  var populatedForms: ListBuffer[TransactionDisplay] = ListBuffer.empty
  var displayedCategoriesHistory: ListBuffer[Seq[Category]] = ListBuffer.empty
  var deletionConfirmations: ListBuffer[CategoryDeletionPreview] = ListBuffer.empty
  var confirmDeletionResult: Boolean = true

  override def displayTransactions(t: Seq[TransactionDisplay]): Unit = displayedTransactions = t
  override def displayCategories(c: Seq[Category]): Unit =
    displayedCategories = c
    displayedCategoriesHistory += c
  override def displayError(m: String): Unit = errors += m
  override def confirmCategoryDeletion(preview: CategoryDeletionPreview): Boolean =
    deletionConfirmations += preview
    confirmDeletionResult
  override def resetForm(): Unit = formResets += 1
  override def populateForm(t: TransactionDisplay): Unit = populatedForms += t

/**
 * Test double for the dashboard view that captures summary output.
 */
final class MockDashboardView extends DashboardViewContract:
  var lastSummary: Option[DashboardDisplay] = None

  override def displaySummary(summary: DashboardDisplay): Unit = lastSummary = Some(summary)

/**
 * Test double for the analytics view that captures chart data.
 */
final class MockAnalyticsView extends AnalyticsViewContract:
  var categoryData: Seq[(String, Double)] = Seq.empty
  var trendData: Option[TrendData] = None

  override def displayCategoryBreakdown(data: Seq[(String, Double)]): Unit = categoryData = data
  override def displaySpendingTrend(data: TrendData): Unit = trendData = Some(data)
