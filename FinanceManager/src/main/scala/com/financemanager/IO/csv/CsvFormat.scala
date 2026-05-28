package com.financemanager.IO.csv

object CsvFormat:
  def renderRow(fields: Seq[String]): String =
    fields.map(escape).mkString(",")

  def parseLine(line: String): Either[String, Seq[String]] =
    val result = collection.mutable.ListBuffer.empty[String]
    val current = new StringBuilder
    var i = 0
    var inQuotes = false

    while i < line.length do
      val ch = line.charAt(i)
      if inQuotes then
        ch match
          case '\\' =>
            if i + 1 < line.length then
              line.charAt(i + 1) match
                case 'n' => current.append('\n')
                case 'r' => current.append('\r')
                case other => current.append(other)
              i += 1
          case '"' =>
            inQuotes = false
          case _ =>
            current.append(ch)
      else
        ch match
          case ',' =>
            result += current.toString
            current.clear()
          case '"' =>
            inQuotes = true
          case _ =>
            current.append(ch)
      i += 1

    if inQuotes then Left("Unterminated quoted field")
    else
      result += current.toString
      Right(result.toList)

  private def escape(value: String): String =
    val needsQuoting = value.exists(ch => ch == ',' || ch == '"' || ch == '\n' || ch == '\r')
    if needsQuoting then
      "\"" + value
        .replace("\\", "\\\\")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\"", "\\\"") + "\""
    else value
