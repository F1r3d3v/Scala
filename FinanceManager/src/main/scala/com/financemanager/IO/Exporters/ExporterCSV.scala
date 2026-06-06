package com.financemanager.IO.Exporters

import com.financemanager.domain.error.IOError
import com.financemanager.traits.Writer
import kantan.csv.*
import kantan.csv.ops.*

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path, StandardOpenOption}
import scala.util.Try

final class ExporterCSV[T](path: Path, includeHeader: Boolean = true)(using encoder: HeaderEncoder[T]) extends Writer[Seq[T]]:
  override def write(data: Seq[T]): Either[IOError, Unit] =
    Try {
      val conf = if includeHeader then rfc.withHeader else rfc
      val content = data.asCsv(conf)
      val parent = path.getParent
      if parent != null then Files.createDirectories(parent)
      Files.write(path, content.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
      ()
    }.toEither.left.map(ex => IOError.WriteError(path.toString, ex.getMessage))
