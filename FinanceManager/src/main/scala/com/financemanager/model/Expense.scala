package com.financemanager.model

import java.time.LocalDate

/**
 * Represents a persisted expense entry displayed in the application.
 *
 * @param id unique identifier assigned by the data source
 * @param date calendar date when the expense happened
 * @param amount expense amount in currency units
 * @param category user-facing category label
 * @param description free-text description of the expense
 */
final case class Expense(
    id: Long,
    date: LocalDate,
    amount: BigDecimal,
    category: String,
    description: String
)

/**
 * Represents user-provided expense data before persistence.
 *
 * @param date calendar date when the expense happened
 * @param amount expense amount in currency units
 * @param category user-facing category label
 * @param description free-text description of the expense
 */
final case class ExpenseInput(
    date: LocalDate,
    amount: BigDecimal,
    category: String,
    description: String
)

