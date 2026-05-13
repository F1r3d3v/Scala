package com.financemanager.domain.service

import com.financemanager.domain.error.DomainError
import com.financemanager.domain.model.TransactionInput
import com.financemanager.domain.model.{Transaction, TransactionId}
import com.financemanager.domain.repository.TransactionRepository
import com.financemanager.domain.model.CategoryId

/**
 * Application service responsible for validating and persisting transactions.
 *
 * @param repository backing repository used for persistence and notifications
 */
final class TransactionService(repository: TransactionRepository):

  def add(input: TransactionInput): Either[DomainError, Transaction] =
    validate(input).map(_ => repository.add(input))

  def update(id: TransactionId, input: TransactionInput): Either[DomainError, Transaction] =
    for
      _ <- validate(input)
      result <- repository.replace(id, input)
    yield result

  def delete(id: TransactionId): Either[DomainError, Unit] =
    repository.remove(id)

  private def validate(input: TransactionInput): Either[DomainError, Unit] =
    if input.amount <= 0 then Left(DomainError.InvalidAmount("Amount must be greater than zero"))
    else if input.category.value <= 0 then Left(DomainError.InvalidCategory("Category is required"))
    else if input.description.trim.isEmpty then Left(DomainError.InvalidDescription("Description is required"))
    else Right(())
