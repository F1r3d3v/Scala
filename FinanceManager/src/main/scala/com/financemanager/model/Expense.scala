package com.financemanager.model

import java.time.LocalDate

final case class Expense(
    id: Long,
    date: LocalDate,
    amount: BigDecimal,
    category: String,
    description: String
)

final case class ExpenseInput(
    date: LocalDate,
    amount: BigDecimal,
    category: String,
    description: String
)

