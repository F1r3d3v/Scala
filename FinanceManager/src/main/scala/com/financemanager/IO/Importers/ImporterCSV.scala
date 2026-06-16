package com.financemanager.IO.Importers

import com.financemanager.domain.error.IOError
import com.financemanager.traits.Loader
import kantan.csv.*
import kantan.csv.ops.*

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}
import scala.util.Try

final class ImporterCSV[T](path: Path, hasHeader: Boolean = true)(using
    decoder: HeaderDecoder[T]
) extends Loader[Seq[T]]:
  override def load(): Either[IOError, Seq[T]] =
    if !Files.exists(path) then Left(IOError.FileNotFound(path.toString))
    else
      Try {
        val content =
          new String(Files.readAllBytes(path), StandardCharsets.UTF_8)
        if content.isBlank then Left(IOError.EndOfFile(path.toString))
        else
          val conf = if hasHeader then rfc.withHeader else rfc
          content
            .readCsv[List, T](conf)
            .foldLeft[Either[IOError, List[T]]](Right(Nil)) { (acc, row) =>
              for
                values <- acc
                value <- row.left.map(err =>
                  IOError.ReadError(path.toString, err.toString)
                )
              yield value :: values
            }
            .map(_.reverse)
      }.toEither.left
        .map(ex => IOError.ReadError(path.toString, ex.getMessage))
        .flatMap(identity)
