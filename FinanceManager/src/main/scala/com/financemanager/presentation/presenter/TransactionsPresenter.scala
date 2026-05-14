package com.financemanager.presentation.presenter

import com.financemanager.domain.model.{CategoryId, Transaction, TransactionId, TransactionInput, TransactionType}
import com.financemanager.domain.repository.TransactionRepository
import com.financemanager.domain.service.{CategoryService, TransactionService}
import com.financemanager.presentation.*
import com.financemanager.presentation.DisplayModels.*

/**
 * Presenter coordinating transaction CRUD between view and services.
 *
 * @param view transactions UI contract
 * @param transactionService domain service for validation/persistence
 * @param categoryService domain service for category lookup
 * @param repository repository to observe for updates
 */
final class TransactionsPresenter(
    view: TransactionsViewContract,
    transactionService: TransactionService,
    categoryService: CategoryService,
    repository: TransactionRepository
) extends TransactionsPresenterContract:

  private var selectedId: Option[TransactionId] = None
  private var categoryCache: Map[CategoryId, String] = Map.empty

  override def onViewCreated(): Unit =
    view.displayCategories(categoryService.getAll)
    refreshTransactions()
    repository.subscribe(() => refreshTransactions())

  override def onSubmit(input: TransactionInput): Unit =
    selectedId match
      case Some(id) =>
        transactionService.update(id, input) match
          case Left(err) => view.displayError(err.message)
          case Right(_) =>
            selectedId = None
            view.resetForm()
      case None =>
        transactionService.add(input) match
          case Left(err) => view.displayError(err.message)
          case Right(_) => view.resetForm()

  override def onDelete(id: TransactionId): Unit =
    transactionService.delete(id) match
      case Left(err) => view.displayError(err.message)
      case Right(_) =>
        selectedId = None
        view.resetForm()

  override def onTransactionSelected(id: TransactionId): Unit =
    selectedId = Some(id)
    repository.findById(id) match
      case Some(t) =>
        view.populateForm(buildTransactionDisplay(t))
      case None => view.displayError(s"Transaction ${id.value} not found")

  override def onClearSelection(): Unit =
    selectedId = None
    view.resetForm()

  private def refreshTransactions(): Unit =
    categoryCache = categoryService.getAll.map(c => c.id -> c.name).toMap
    val displays = repository.findAll().map(buildTransactionDisplay)
    view.displayTransactions(displays)

  private def buildTransactionDisplay(t: Transaction): TransactionDisplay =
    val categoryName = categoryCache.getOrElse(t.category, s"Unknown")
    val prefix = if t.transactionType == TransactionType.Income then "+$" else "-$"
    TransactionDisplay(t.id, t.date.toString, prefix + f"${t.amount.toDouble}%.2f", t.amount, categoryName, t.description, t.transactionType)
