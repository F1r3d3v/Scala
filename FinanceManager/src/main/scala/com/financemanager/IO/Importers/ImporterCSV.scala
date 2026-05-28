package com.financemanager.IO.Importers

import com.financemanager.domain.error.IOError
import com.financemanager.traits.Loader
import com.financemanager.IO.csv.{CsvCodec, CsvFormat}

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}
import scala.jdk.CollectionConverters.*
import scala.util.Try

final class ImporterCSV[T](path: Path, codec: CsvCodec[T], hasHeader: Boolean = true) extends Loader[Seq[T]]:
  override def load(): Either[IOError, Seq[T]] =
    if !Files.exists(path) then Left(IOError.FileNotFound(path.toString))
    else
      Try {
        val lines = Files.readAllLines(path, StandardCharsets.UTF_8).asScala.toList
        val dataLines =
          if hasHeader then
            validateHeader(lines).map(_ => lines.drop(1))
          else Right(lines)

        dataLines.flatMap(parseLines)
      }.toEither
        .left.map(ex => IOError.ReadError(path.toString, ex.getMessage))
        .flatMap(identity)

  private def validateHeader(lines: List[String]): Either[IOError, Unit] =
    lines.headOption match
      case None => Left(IOError.EndOfFile(path.toString))
      case Some(headerLine) =>
        CsvFormat.parseLine(headerLine).left.map(err => IOError.ReadError(path.toString, err)).flatMap { header =>
          val normalized = header.map(_.trim)
          if normalized == codec.headers then Right(())
          else Left(IOError.ReadError(path.toString, s"Invalid CSV header. Expected ${codec.headers.mkString(",")}"))
        }

  private def parseLines(lines: List[String]): Either[IOError, Seq[T]] =
    val parsed = lines.zipWithIndex.collect {
      case (line, index) if line.trim.nonEmpty =>
        val lineNumber = if hasHeader then index + 2 else index + 1
        parseLine(line, lineNumber)
    }

    parsed.foldLeft[Either[IOError, List[T]]](Right(Nil)) { (acc, next) =>
      for
        values <- acc
        value <- next
      yield value :: values
    }.map(_.reverse)

  private def parseLine(line: String, lineNumber: Int): Either[IOError, T] =
    CsvFormat.parseLine(line)
      .left.map(err => IOError.ReadError(path.toString, s"Line $lineNumber: $err"))
      .flatMap(fields => codec.decode(fields).left.map(err => IOError.ReadError(path.toString, s"Line $lineNumber: ${decodeMessage(err)}")))

  private def decodeMessage(error: IOError): String =
    error match
      case IOError.ReadError(_, cause) => cause
      case other => other.message
