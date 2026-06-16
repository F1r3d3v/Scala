package com.financemanager.domain

import com.financemanager.IO.Exporters.ExporterCSV
import com.financemanager.IO.Importers.ImporterCSV
import com.financemanager.IO.csv.TransactionCsvCodec.{*, given}
import com.financemanager.IO.csv.TransactionCsvRow
import com.financemanager.domain.model.{
  Category,
  CategoryId,
  ImportMode,
  Transaction,
  TransactionId,
  TransactionType
}
import com.financemanager.domain.repository.InMemoryCategoryRepository
import com.financemanager.domain.service.{
  CategoryService,
  ImportExportService,
  TransactionService
}
import com.financemanager.testutil.TestRepository
import kantan.csv.*
import kantan.csv.ops.*

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.time.LocalDate
import munit.FunSuite

class ImportExportServiceTest extends FunSuite:

  private def buildService(
      repo: com.financemanager.domain.repository.TransactionRepository,
      categories: Seq[Category] = Seq(
        Category(CategoryId(1L), "Food"),
        Category(CategoryId(4L), "Gift"),
        Category(CategoryId(2L), "Coffee"),
        Category(CategoryId(3L), "Salary")
      )
  ): (ImportExportService, CategoryService) =
    val categoryService = new CategoryService(
      new InMemoryCategoryRepository(categories)
    )
    (
      new ImportExportService(repo, TransactionService(repo), categoryService),
      categoryService
    )

  test("export writes CSV rows with header"):
    val transactions = Seq(
      Transaction(
        TransactionId(1L),
        LocalDate.parse("2024-01-01"),
        BigDecimal("10.00"),
        CategoryId(2L),
        "Coffee",
        TransactionType.Expense
      ),
      Transaction(
        TransactionId(2L),
        LocalDate.parse("2024-01-02"),
        BigDecimal("2500.00"),
        CategoryId(3L),
        "Salary",
        TransactionType.Income
      )
    )
    val repo = TestRepository(transactions*)
    val tempFile = Files.createTempFile("transactions", ".csv")

    val exporter = ExporterCSV(tempFile)

    val (service, _) = buildService(repo)
    val result = service.exportTransactions(exporter)
    assert(result.isRight, clue = "expected export to succeed")

    val expected = List(
      TransactionCsvRow(
        transactions.head.date,
        transactions.head.amount,
        "Coffee",
        transactions.head.description,
        transactions.head.transactionType
      ),
      TransactionCsvRow(
        transactions(1).date,
        transactions(1).amount,
        "Salary",
        transactions(1).description,
        transactions(1).transactionType
      )
    ).asCsv(rfc.withHeader)

    val actual =
      new String(Files.readAllBytes(tempFile), StandardCharsets.UTF_8)
    assertEquals(
      actual,
      expected,
      clue = "expected CSV contents to match exported transactions"
    )

  test("import reads CSV rows and persists transactions"):
    val tempFile = Files.createTempFile("transactions", ".csv")
    val input = Seq(
      TransactionCsvRow(
        LocalDate.parse("2024-02-01"),
        BigDecimal("15.50"),
        "Food",
        "Lunch",
        TransactionType.Expense
      ),
      TransactionCsvRow(
        LocalDate.parse("2024-02-05"),
        BigDecimal("120.00"),
        "Gift",
        "Gift",
        TransactionType.Income
      )
    )

    val csvContent = List(input.head, input(1)).asCsv(rfc.withHeader)
    Files.write(tempFile, csvContent.getBytes(StandardCharsets.UTF_8))

    val repo = TestRepository()
    val importer = ImporterCSV(tempFile)

    val (service, _) = buildService(repo)
    val result = service.importTransactions(importer, ImportMode.Overwrite)
    assert(result.isRight, clue = "expected import to succeed")
    assertEquals(
      repo.findAll().size,
      2,
      clue = "expected imported transactions to be persisted"
    )
    assertEquals(repo.findAll().head.description, "Lunch")
    assertEquals(repo.findAll().last.amount, BigDecimal("120.00"))

  test("append import keeps existing transactions and adds imported ones"):
    val tempFile = Files.createTempFile("transactions", ".csv")
    val existing = Transaction(
      TransactionId(7L),
      LocalDate.parse("2024-01-10"),
      BigDecimal("50.00"),
      CategoryId(3L),
      "Existing",
      TransactionType.Expense
    )
    val importedInput = Seq(
      TransactionCsvRow(
        LocalDate.parse("2024-02-01"),
        BigDecimal("15.50"),
        "Food",
        "Lunch",
        TransactionType.Expense
      )
    )

    val csvContent = importedInput.asCsv(rfc.withHeader)
    Files.write(tempFile, csvContent.getBytes(StandardCharsets.UTF_8))

    val repo = TestRepository(existing)
    val importer = ImporterCSV(tempFile)

    val (service, _) = buildService(repo)
    val result = service.importTransactions(importer, ImportMode.Append)

    assert(result.isRight, clue = "expected append import to succeed")
    assertEquals(repo.findAll().map(_.description), Seq("Existing", "Lunch"))

  test("overwrite import replaces existing transactions"):
    val tempFile = Files.createTempFile("transactions", ".csv")
    val existing = Transaction(
      TransactionId(7L),
      LocalDate.parse("2024-01-10"),
      BigDecimal("50.00"),
      CategoryId(3L),
      "Existing",
      TransactionType.Expense
    )
    val importedInput = Seq(
      TransactionCsvRow(
        LocalDate.parse("2024-02-01"),
        BigDecimal("15.50"),
        "Food",
        "Lunch",
        TransactionType.Expense
      )
    )

    val csvContent = importedInput.asCsv(rfc.withHeader)
    Files.write(tempFile, csvContent.getBytes(StandardCharsets.UTF_8))

    val repo = TestRepository(existing)
    val importer = ImporterCSV(tempFile)

    val (service, _) = buildService(repo)
    val result = service.importTransactions(importer, ImportMode.Overwrite)

    assert(result.isRight, clue = "expected overwrite import to succeed")
    assertEquals(repo.findAll().map(_.description), Seq("Lunch"))

  test("import creates missing category based on CSV name"):
    val tempFile = Files.createTempFile("transactions", ".csv")
    val input = Seq(
      TransactionCsvRow(
        LocalDate.parse("2024-02-01"),
        BigDecimal("15.50"),
        "New Category",
        "Lunch",
        TransactionType.Expense
      )
    )

    val csvContent = input.asCsv(rfc.withHeader)
    Files.write(tempFile, csvContent.getBytes(StandardCharsets.UTF_8))

    val repo = TestRepository()
    val (service, categoryService) =
      buildService(repo, Seq(Category(CategoryId(1L), "Food")))
    val importer = ImporterCSV(tempFile)

    val result = service.importTransactions(importer, ImportMode.Append)

    assert(
      result.isRight,
      clue = "expected import with new category to succeed"
    )
    assertEquals(
      categoryService.findByName("New Category").map(_.name),
      Some("New Category")
    )
    assertEquals(repo.findAll().head.description, "Lunch")
