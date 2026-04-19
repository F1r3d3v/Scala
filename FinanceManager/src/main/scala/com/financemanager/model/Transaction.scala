package com.financemanager.model

import java.time.LocalDate

/**
 * Represents a persisted transaction entry displayed in the application.
 *
 * @param id unique identifier assigned by the data source
 * @param date calendar date when the transaction happened
 * @param amount transaction amount in currency units
 * @param category user-facing category label
 * @param description free-text description of the transaction
 * @param isIncome flag indicating whether this entry is an income (true) or an expense (false)
 */
final case class Transaction(
    id: Long,
    date: LocalDate,
    amount: BigDecimal,
    category: String,
    description: String,
    isIncome: Boolean
)

/**
 * Represents user-provided transaction data before persistence.
 *
 * @param date calendar date when the transaction happened
 * @param amount transaction amount in currency units
 * @param category user-facing category label
 * @param description free-text description of the transaction
 * @param isIncome flag indicating whether this entry is an income (true) or an expense (false)
 */
final case class TransactionInput(
    date: LocalDate,
    amount: BigDecimal,
    category: String,
    description: String,
    isIncome: Boolean
)

