package com.financemanager.domain.repository

import com.financemanager.domain.error.DomainError
import com.financemanager.domain.model.{Transaction, TransactionId, TransactionInput}
import com.financemanager.domain.repository.TransactionRepository

/**
 * In-memory repository for transactions, primarily for demos and tests.
 *
 * @param initialData seed data loaded at startup
 */
final class InMemoryTransactionRepository(initialData: Seq[Transaction] = Seq.empty)
    extends TransactionRepository:

  private var transactions: Vector[Transaction] = initialData.toVector
  private var nextRawId: Long = initialData.map(_.id.value).maxOption.getOrElse(0L) + 1

  override def findAll(): Seq[Transaction] = transactions

  override def findById(id: TransactionId): Option[Transaction] = transactions.find(_.id == id)

  override def add(input: TransactionInput): Transaction =
    val t = Transaction(TransactionId(nextRawId), input.date, input.amount, input.category, input.description, input.transactionType)
    nextRawId += 1
    transactions = transactions :+ t
    notifyListeners()
    t

  override def replace(id: TransactionId, input: TransactionInput): Either[DomainError, Transaction] =
    transactions.indexWhere(_.id == id) match
      case -1 => Left(DomainError.NotFound(id))
      case idx =>
        val t = Transaction(id, input.date, input.amount, input.category, input.description, input.transactionType)
        transactions = transactions.updated(idx, t)
        notifyListeners()
        Right(t)

  override def remove(id: TransactionId): Either[DomainError, Unit] =
    transactions.find(_.id == id) match
      case None => Left(DomainError.NotFound(id))
      case _ =>
        transactions = transactions.filterNot(_.id == id)
        notifyListeners()
        Right(())



