package com.financemanager.presentation.presenter

import com.financemanager.domain.model.Extensions.*
import com.financemanager.domain.repository.TransactionRepository
import com.financemanager.domain.service.BudgetService
import com.financemanager.presentation.*
import com.financemanager.presentation.DisplayModels.*

/** Presenter that prepares dashboard summary data for the view.
  *
  * @param view
  *   dashboard UI contract
  * @param budgetService
  *   domain service that computes summary metrics
  * @param repository
  *   repository to observe for updates
  */
final class DashboardPresenter(
    view: DashboardViewContract,
    budgetService: BudgetService,
    repository: TransactionRepository
) extends DashboardPresenterContract:

  override def onViewCreated(): Unit =
    refresh()
    repository.subscribe(() => refresh())

  private def refresh(): Unit =
    val summary = budgetService.computeSummary()
    view.displaySummary(
      DashboardDisplay(
        totalSpent = summary.totalSpent.moneyFormat,
        totalIncome = summary.totalIncome.moneyFormat,
        budgetRemaining = summary.remaining.moneyFormat,
        transactionCount = summary.transactionCount
      )
    )
