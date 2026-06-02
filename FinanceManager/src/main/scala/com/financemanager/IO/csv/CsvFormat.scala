package com.financemanager.IO.csv

object CsvFormat:
  val CRLF = "\r\n"

  def renderRow(fields: Seq[String]): String =
    fields.map(escape).mkString(",")

  def parseRecords(content: String): Either[String, Seq[Seq[String]]] =
    val records = collection.mutable.ListBuffer.empty[Seq[String]]
    val fields = collection.mutable.ListBuffer.empty[String]
    val current = new StringBuilder
    var i = 0
    var inQuotes = false
    var fieldHasContent = false
    var recordHasFields = false

    while i < content.length do
      val ch = content.charAt(i)
      if inQuotes then
        ch match
          case '"' =>
            if i + 1 < content.length && content.charAt(i + 1) == '"' then
              current.append('"')
              fieldHasContent = true
              i += 1
            else inQuotes = false
          case _ =>
            current.append(ch)
            fieldHasContent = true
      else
        ch match
          case ',' =>
            fields += current.toString
            current.clear()
            recordHasFields = true
            fieldHasContent = false
          case '"' =>
            if fieldHasContent then return Left(s"Unexpected quote at position $i in unquoted field")
            inQuotes = true
          case '\r' =>
            if i + 1 < content.length && content.charAt(i + 1) == '\n' then
              i += 1
            fields += current.toString
            records += fields.toList
            fields.clear()
            current.clear()
            fieldHasContent = false
            recordHasFields = false
          case '\n' =>
            fields += current.toString
            records += fields.toList
            fields.clear()
            current.clear()
            fieldHasContent = false
            recordHasFields = false
          case _ =>
            current.append(ch)
            fieldHasContent = true
      i += 1

    if inQuotes then Left("Unterminated quoted field")
    else
      if fieldHasContent || recordHasFields then
        fields += current.toString
        records += fields.toList
      Right(records.toList)

  def parseLine(line: String): Either[String, Seq[String]] =
    val result = collection.mutable.ListBuffer.empty[String]
    val current = new StringBuilder
    var i = 0
    var inQuotes = false

    while i < line.length do
      val ch = line.charAt(i)
      if inQuotes then
        ch match
          case '"' =>
            if i + 1 < line.length && line.charAt(i + 1) == '"' then
              current.append('"')
              i += 1
            else inQuotes = false
          case _ =>
            current.append(ch)
      else
        ch match
          case ',' =>
            result += current.toString
            current.clear()
          case '"' =>
            if current.nonEmpty then return Left(s"Unexpected quote at position $i in unquoted field")
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
      "\"" + value.replace("\"", "\"\"") + "\""
    else value
