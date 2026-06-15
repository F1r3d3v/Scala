package com.financemanager.domain.model

import com.financemanager.domain.model.CategoryId
import java.time.LocalDate

/** Unique identifier for a persisted transaction.
  */
opaque type TransactionId = Long

object TransactionId:
  def apply(value: Long): TransactionId = value
  extension (id: TransactionId) def value: Long = id

/** Represents a persisted transaction entry displayed in the application.
  *
  * @param id
  *   unique identifier assigned by the data source
  * @param date
  *   calendar date when the transaction happened
  * @param amount
  *   transaction amount in currency units
  * @param category
  *   user-facing category label
  * @param description
  *   free-text description of the transaction
  * @param transactionType
  *   classification as income or expense
  */
final case class Transaction(
    id: TransactionId,
    date: LocalDate,
    amount: BigDecimal,
    category: CategoryId,
    description: String,
    transactionType: TransactionType
)

/** Represents user-provided transaction data before persistence.
  *
  * @param date
  *   calendar date when the transaction happened
  * @param amount
  *   transaction amount in currency units
  * @param category
  *   user-facing category label
  * @param description
  *   free-text description of the transaction
  * @param transactionType
  *   classification as income or expense
  */
final case class TransactionInput(
    date: LocalDate,
    amount: BigDecimal,
    category: CategoryId,
    description: String,
    transactionType: TransactionType
)

/** Classification of a transaction as income or expense.
  */
enum TransactionType:
  case Income, Expense
