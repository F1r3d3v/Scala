package com.financemanager.domain.repository

import com.financemanager.domain.error.DomainError
import com.financemanager.domain.model.{CategoryId, ImportMode, Transaction, TransactionId, TransactionInput}

/**
 * Repository contract for persisted transactions.
 */
trait TransactionRepository extends Subscribable:
  def findAll(): Seq[Transaction]
  def findById(id: TransactionId): Option[Transaction]
  def add(input: TransactionInput): Transaction
  def importBatch(inputs: Seq[TransactionInput], mode: ImportMode): Seq[Transaction]
  /**
   * Reassigns every transaction from one category to another.
   */
  def reassignCategory(from: CategoryId, to: CategoryId): Unit
  def replace(id: TransactionId, input: TransactionInput): Either[DomainError, Transaction]
  def remove(id: TransactionId): Either[DomainError, Unit]
  def removeAll(): Unit
