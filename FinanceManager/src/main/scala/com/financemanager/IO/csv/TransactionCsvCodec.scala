package com.financemanager.IO.csv

import com.financemanager.domain.error.IOError
import com.financemanager.domain.model.{CategoryId, TransactionInput, TransactionType}

import java.time.LocalDate
import scala.util.Try

object TransactionCsvCodec:
  lazy val transaction: CsvCodec[TransactionInput] = new CsvCodec[TransactionInput]:
    override val headers: Seq[String] = Seq("date", "amount", "categoryId", "description", "transactionType")

    override def encode(value: TransactionInput): Seq[String] =
      Seq(
        value.date.toString,
        value.amount.toString,
        value.category.value.toString,
        value.description,
        value.transactionType.toString
      )

    override def decode(fields: Seq[String]): Either[IOError, TransactionInput] =
      if fields.length != 5 then Left(IOError.ReadError("", s"Expected 5 fields, found ${fields.length}"))
      else
        for
          date <- parseDate(fields.head)
          amount <- parseAmount(fields(1))
          category <- parseLong(fields(2)).map(CategoryId.apply)
          description <- parseDescription(fields(3))
          tpe <- parseType(fields(4))
        yield TransactionInput(date, amount, category, description, tpe)

  private def parseDate(raw: String): Either[IOError, LocalDate] =
    Try(LocalDate.parse(raw.trim)).toEither.left.map(ex => IOError.ReadError("", s"Invalid date '$raw': ${ex.getMessage}"))

  private def parseAmount(raw: String): Either[IOError, BigDecimal] =
    Try(BigDecimal(raw.trim)).toEither
      .left.map(ex => IOError.ReadError("", s"Invalid amount '$raw': ${ex.getMessage}"))
      .flatMap { amount =>
        if amount <= 0 then Left(IOError.ReadError("", "Amount must be greater than zero"))
        else Right(amount)
      }

  private def parseLong(raw: String): Either[IOError, Long] =
    Try(raw.trim.toLong).toEither
      .left.map(ex => IOError.ReadError("", s"Invalid number '$raw': ${ex.getMessage}"))
      .flatMap { value =>
        if value <= 0 then Left(IOError.ReadError("", s"Value must be positive: $raw"))
        else Right(value)
      }

  private def parseDescription(raw: String): Either[IOError, String] =
    val trimmed = raw.trim
    if trimmed.nonEmpty then Right(trimmed) else Left(IOError.ReadError("", "Description is required"))

  private def parseType(raw: String): Either[IOError, TransactionType] =
    raw.trim.toLowerCase match
      case "income" => Right(TransactionType.Income)
      case "expense" => Right(TransactionType.Expense)
      case other => Left(IOError.ReadError("", s"Unknown transaction type '$other'"))
