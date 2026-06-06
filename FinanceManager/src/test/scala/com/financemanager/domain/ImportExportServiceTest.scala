package com.financemanager.domain

import com.financemanager.IO.Exporters.ExporterCSV
import com.financemanager.IO.Importers.ImporterCSV
import com.financemanager.IO.csv.TransactionCsvCodec.{*, given}
import com.financemanager.domain.model.{CategoryId, Transaction, TransactionId, TransactionInput, TransactionType}
import com.financemanager.domain.service.{ImportExportService, TransactionService}
import com.financemanager.testutil.TestRepository
import kantan.csv.*
import kantan.csv.ops.*

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.time.LocalDate
import munit.FunSuite

class ImportExportServiceTest extends FunSuite:

  test("export writes CSV rows with header"):
    val transactions = Seq(
      Transaction(TransactionId(1L), LocalDate.parse("2024-01-01"), BigDecimal("10.00"), CategoryId(2L), "Coffee", TransactionType.Expense),
      Transaction(TransactionId(2L), LocalDate.parse("2024-01-02"), BigDecimal("2500.00"), CategoryId(3L), "Salary", TransactionType.Income)
    )
    val repo = TestRepository(transactions*)
    val tempFile = Files.createTempFile("transactions", ".csv")

    val exporter = ExporterCSV(tempFile)

    val service = ImportExportService(repo, TransactionService(repo))
    val result = service.exportTransactions(exporter)
    assert(result.isRight, clue = "expected export to succeed")

    val expected = List(
      TransactionInput(
        transactions.head.date,
        transactions.head.amount,
        transactions.head.category,
        transactions.head.description,
        transactions.head.transactionType
      ),
      TransactionInput(
        transactions(1).date,
        transactions(1).amount,
        transactions(1).category,
        transactions(1).description,
        transactions(1).transactionType
      )
    ).asCsv(rfc.withHeader)

    val actual = new String(Files.readAllBytes(tempFile), StandardCharsets.UTF_8)
    assertEquals(actual, expected, clue = "expected CSV contents to match exported transactions")

  test("import reads CSV rows and persists transactions"):
    val tempFile = Files.createTempFile("transactions", ".csv")
    val input = Seq(
      TransactionInput(LocalDate.parse("2024-02-01"), BigDecimal("15.50"), CategoryId(1L), "Lunch", TransactionType.Expense),
      TransactionInput(LocalDate.parse("2024-02-05"), BigDecimal("120.00"), CategoryId(4L), "Gift", TransactionType.Income)
    )

    val csvContent = List(input.head, input(1)).asCsv(rfc.withHeader)
    Files.write(tempFile, csvContent.getBytes(StandardCharsets.UTF_8))

    val repo = TestRepository()
    val importer = ImporterCSV(tempFile)

    val service = ImportExportService(repo, TransactionService(repo))
    val result = service.importTransactions(importer)
    assert(result.isRight, clue = "expected import to succeed")
    assertEquals(repo.findAll().size, 2, clue = "expected imported transactions to be persisted")
    assertEquals(repo.findAll().head.description, "Lunch")
    assertEquals(repo.findAll().last.amount, BigDecimal("120.00"))
