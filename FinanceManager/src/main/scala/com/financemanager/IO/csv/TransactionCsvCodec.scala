package com.financemanager.IO.csv

import com.financemanager.domain.model.{CategoryId, TransactionInput, TransactionType}
import kantan.csv.*
import kantan.csv.java8.*

import java.time.LocalDate

object TransactionCsvCodec:

  given CellDecoder[TransactionType] =
    CellDecoder.from(s =>
      s.trim.toLowerCase match
        case "income"  => Right(TransactionType.Income)
        case "expense" => Right(TransactionType.Expense)
        case other     => Left(DecodeError.TypeError(s"Not a valid TransactionType: '$other'"))
    )

  given CellEncoder[TransactionType] =
    CellEncoder.from(t => t.toString)

  given HeaderDecoder[TransactionInput] =
    HeaderDecoder.decoder("date", "amount", "categoryId", "description", "transactionType")(
      (date: LocalDate, amount: BigDecimal, categoryId: Long, description: String, transactionType: TransactionType) =>
        TransactionInput(date, amount, CategoryId(categoryId), description, transactionType)
    )

  given HeaderEncoder[TransactionInput] =
    HeaderEncoder.encoder("date", "amount", "categoryId", "description", "transactionType")(
      (t: TransactionInput) => (t.date, t.amount, t.category.value, t.description, t.transactionType)
    )
