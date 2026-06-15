package com.financemanager.testutil

import com.financemanager.domain.error.DomainError
import com.financemanager.domain.model.{
  CategoryId,
  ImportMode,
  Transaction,
  TransactionId,
  TransactionInput
}
import com.financemanager.domain.repository.TransactionRepository

/** Factory for lightweight in-memory repositories used in tests.
  */
object TestRepository:
  def apply(data: Transaction*): TransactionRepository =
    new TransactionRepository:
      private var transactions = data.toList
      private var nextId = data.map(_.id.value).maxOption.getOrElse(0L) + 1

      override def findAll(): Seq[Transaction] = transactions

      override def findById(id: TransactionId): Option[Transaction] =
        transactions.find(_.id == id)

      override def countByCategory(categoryId: CategoryId): Int =
        transactions.count(_.category == categoryId)

      override def add(input: TransactionInput): Transaction =
        val t = Transaction(
          TransactionId(nextId),
          input.date,
          input.amount,
          input.category,
          input.description,
          input.transactionType
        )
        nextId += 1
        transactions = transactions :+ t
        notifyListeners()
        t

      override def importBatch(
          inputs: Seq[TransactionInput],
          mode: ImportMode
      ): Seq[Transaction] =
        if mode == ImportMode.Overwrite then transactions = Nil

        val imported = inputs.map { input =>
          val transaction = Transaction(
            TransactionId(nextId),
            input.date,
            input.amount,
            input.category,
            input.description,
            input.transactionType
          )
          nextId += 1
          transaction
        }

        transactions = transactions ++ imported
        notifyListeners()
        imported

      override def reassignCategory(from: CategoryId, to: CategoryId): Unit =
        transactions = transactions.map(transaction =>
          if transaction.category == from then transaction.copy(category = to)
          else transaction
        )
        notifyListeners()

      override def replace(
          id: TransactionId,
          input: TransactionInput
      ): Either[DomainError, Transaction] =
        transactions.indexWhere(_.id == id) match
          case -1  => Left(DomainError.NotFound(id))
          case idx =>
            val t = Transaction(
              id,
              input.date,
              input.amount,
              input.category,
              input.description,
              input.transactionType
            )
            transactions = transactions.updated(idx, t)
            notifyListeners()
            Right(t)

      override def remove(id: TransactionId): Either[DomainError, Unit] =
        transactions.find(_.id == id) match
          case None => Left(DomainError.NotFound(id))
          case _    =>
            transactions = transactions.filterNot(_.id == id)
            notifyListeners()
            Right(())

      override def removeAll(): Unit =
        transactions = Nil
        notifyListeners()
