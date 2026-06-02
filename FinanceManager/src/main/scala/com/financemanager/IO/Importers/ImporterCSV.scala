package com.financemanager.IO.Importers

import com.financemanager.domain.error.IOError
import com.financemanager.traits.Loader
import com.financemanager.IO.csv.{CsvCodec, CsvFormat}

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}
import scala.util.Try

final class ImporterCSV[T](path: Path, codec: CsvCodec[T], hasHeader: Boolean = true) extends Loader[Seq[T]]:
  override def load(): Either[IOError, Seq[T]] =
    if !Files.exists(path) then Left(IOError.FileNotFound(path.toString))
    else
      Try {
        val content = new String(Files.readAllBytes(path), StandardCharsets.UTF_8)
        CsvFormat.parseRecords(content).left.map(err => IOError.ReadError(path.toString, err))
      }.toEither
        .left.map(ex => IOError.ReadError(path.toString, ex.getMessage))
        .flatMap(identity)
        .flatMap(records => processRecords(records))

  private def processRecords(records: Seq[Seq[String]]): Either[IOError, Seq[T]] =
    if records.isEmpty then Left(IOError.EndOfFile(path.toString))
    else
      val (headerRow, dataRows) =
        if hasHeader then (records.head, records.drop(1)) else (null, records)

      for
        _ <- if hasHeader then validateHeader(headerRow) else Right(())
        result <- parseRows(dataRows)
      yield result

  private def validateHeader(header: Seq[String]): Either[IOError, Unit] =
    val normalized = header.map(_.trim)
    if normalized == codec.headers then Right(())
    else Left(IOError.ReadError(path.toString, s"Invalid CSV header. Expected ${codec.headers.mkString(",")}"))

  private def parseRows(rows: Seq[Seq[String]]): Either[IOError, Seq[T]] =
    val headerOffset = if hasHeader then 1 else 0
    rows.zipWithIndex.foldLeft[Either[IOError, List[T]]](Right(Nil)) { (acc, pair) =>
      val (fields, index) = pair
      val lineNumber = index + headerOffset + 1
      for
        values <- acc
        value <- codec.decode(fields).left.map(err => IOError.ReadError(path.toString, s"Line $lineNumber: ${decodeMessage(err)}"))
      yield value :: values
    }.map(_.reverse)

  private def decodeMessage(error: IOError): String =
    error match
      case IOError.ReadError(_, cause) => cause
      case other => other.message
