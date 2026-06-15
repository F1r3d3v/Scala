package com.financemanager.IO.csv

import com.financemanager.domain.model.TransactionType
import kantan.csv.*
import kantan.csv.java8.*

import java.time.LocalDate

object TransactionCsvCodec:

  given CellDecoder[TransactionType] =
    CellDecoder.from(s =>
      s.trim.toLowerCase match
        case "income"  => Right(TransactionType.Income)
        case "expense" => Right(TransactionType.Expense)
        case other     =>
          Left(DecodeError.TypeError(s"Not a valid TransactionType: '$other'"))
    )

  given CellEncoder[TransactionType] =
    CellEncoder.from(t => t.toString)

  given HeaderDecoder[TransactionCsvRow] =
    HeaderDecoder.decoder(
      "date",
      "amount",
      "category",
      "description",
      "transactionType"
    )(
      (
          date: LocalDate,
          amount: BigDecimal,
          category: String,
          description: String,
          transactionType: TransactionType
      ) =>
        TransactionCsvRow(date, amount, category, description, transactionType)
    )

  given HeaderEncoder[TransactionCsvRow] =
    HeaderEncoder.encoder(
      "date",
      "amount",
      "category",
      "description",
      "transactionType"
    )((t: TransactionCsvRow) =>
      (t.date, t.amount, t.category, t.description, t.transactionType)
    )
