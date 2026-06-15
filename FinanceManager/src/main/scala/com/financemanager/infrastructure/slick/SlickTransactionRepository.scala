package com.financemanager.infrastructure.slick

import com.financemanager.domain.error.DomainError
import com.financemanager.domain.model.{CategoryId, ImportMode, Transaction, TransactionId, TransactionInput}
import com.financemanager.domain.repository.TransactionRepository
import slick.jdbc.SQLiteProfile.api._
import scala.concurrent.Await
import scala.concurrent.duration.Duration

class SlickTransactionRepository(dbManager: SlickDatabaseManager) extends TransactionRepository:
  import SlickTables._

  private val db = dbManager.db

  override def findAll(): Seq[Transaction] =
    Await.result(db.run(transactions.result), Duration.Inf)

  override def findById(id: TransactionId): Option[Transaction] =
    Await.result(db.run(transactions.filter(_.id === id).result.headOption), Duration.Inf)

  override def add(input: TransactionInput): Transaction =
    val insertQuery = (transactions.map(t => (t.date, t.amount, t.categoryId, t.description, t.transactionType))
      returning transactions.map(_.id)
      into ((tuple, id) => Transaction(id, tuple._1, tuple._2, tuple._3, tuple._4, tuple._5)))

    val action = insertQuery += (input.date, input.amount, input.category, input.description, input.transactionType)
    val result = Await.result(db.run(action), Duration.Inf)

    notifyListeners()
    result

  override def importBatch(inputs: Seq[TransactionInput], mode: ImportMode): Seq[Transaction] =
    val insertQuery = (transactions.map(t => (t.date, t.amount, t.categoryId, t.description, t.transactionType))
      returning transactions.map(_.id)
      into ((tuple, id) => Transaction(id, tuple._1, tuple._2, tuple._3, tuple._4, tuple._5)))

    val deleteExisting =
      if mode == ImportMode.Overwrite then transactions.delete
      else DBIO.successful(0)

    val insertBatch = DBIO.sequence(inputs.map(input =>
      insertQuery += (input.date, input.amount, input.category, input.description, input.transactionType)
    ))

    val imported = Await.result(
      db.run(deleteExisting.andThen(insertBatch).transactionally),
      Duration.Inf
    )

    notifyListeners()
    imported

  override def reassignCategory(from: CategoryId, to: CategoryId): Unit =
    Await.result(
      db.run(
        transactions
          .filter(_.categoryId === from)
          .map(_.categoryId)
          .update(to)
      ),
      Duration.Inf
    )
    notifyListeners()

  override def replace(id: TransactionId, input: TransactionInput): Either[DomainError, Transaction] =
    val updateAction = transactions.filter(_.id === id)
      .map(t => (t.date, t.amount, t.categoryId, t.description, t.transactionType))
      .update((input.date, input.amount, input.category, input.description, input.transactionType))

    val updatedRows = Await.result(db.run(updateAction), Duration.Inf)

    if updatedRows == 0 then Left(DomainError.NotFound(id))
    else
      val updatedTx = Transaction(id, input.date, input.amount, input.category, input.description, input.transactionType)
      notifyListeners()
      Right(updatedTx)

  override def remove(id: TransactionId): Either[DomainError, Unit] =
    val deleteAction = transactions.filter(_.id === id).delete
    val deletedRows = Await.result(db.run(deleteAction), Duration.Inf)

    if deletedRows == 0 then Left(DomainError.NotFound(id))
    else
      notifyListeners()
      Right(())

  override def removeAll(): Unit =
    Await.result(db.run(transactions.delete), Duration.Inf)
    notifyListeners()
