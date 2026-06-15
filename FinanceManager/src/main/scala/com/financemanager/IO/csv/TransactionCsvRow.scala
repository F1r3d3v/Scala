package com.financemanager.IO.csv

import com.financemanager.domain.model.TransactionType

import java.time.LocalDate

/** CSV-facing representation of a transaction using category names.
  */
final case class TransactionCsvRow(
    date: LocalDate,
    amount: BigDecimal,
    category: String,
    description: String,
    transactionType: TransactionType
)
