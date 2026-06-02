package com.financemanager.IO.Exporters

import com.financemanager.domain.error.IOError
import com.financemanager.traits.Writer
import com.financemanager.IO.csv.{CsvCodec, CsvFormat}

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path, StandardOpenOption}
import scala.util.Try

final class ExporterCSV[T](path: Path, codec: CsvCodec[T], includeHeader: Boolean = true) extends Writer[Seq[T]]:
  override def write(data: Seq[T]): Either[IOError, Unit] =
    Try {
      val rows =
        val payload = data.map(codec.encode).map(CsvFormat.renderRow)
        if includeHeader then CsvFormat.renderRow(codec.headers) +: payload else payload

      val content = rows.mkString(CsvFormat.CRLF)
      val parent = path.getParent
      if parent != null then Files.createDirectories(parent)
      Files.write(path, content.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
      ()
    }.toEither.left.map(ex => IOError.WriteError(path.toString, ex.getMessage))
