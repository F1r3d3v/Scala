package com.financemanager.domain.service

import com.financemanager.domain.error.IOError
import com.financemanager.domain.model.{ImportMode, Transaction, TransactionInput}
import com.financemanager.IO.csv.TransactionCsvRow
import com.financemanager.traits.{Loader, Writer}
import com.financemanager.domain.repository.TransactionRepository

final class ImportExportService(
  transactionRepository: TransactionRepository,
  transactionService: TransactionService,
  categoryService: CategoryService
):

  def importTransactions(
    importer: Loader[Seq[TransactionCsvRow]],
    mode: ImportMode
  ): Either[IOError, Seq[Transaction]] =
    importer.load().flatMap { rows =>
      buildImportedTransactions(rows).map { validInputs =>
        transactionRepository.importBatch(validInputs, mode)
      }
    }

  def exportTransactions(exporter: Writer[Seq[TransactionCsvRow]]): Either[IOError, Unit] =
    buildExportRows.flatMap(exporter.write)

  private def buildImportedTransactions(
    rows: Seq[TransactionCsvRow]
  ): Either[IOError, Seq[TransactionInput]] =
    rows.foldLeft[Either[IOError, Seq[TransactionInput]]](Right(Seq.empty)) { (acc, row) =>
      acc.flatMap { validInputs =>
        val normalizedCategory = row.category.trim
        if normalizedCategory.isEmpty then Left(IOError.ReadError("", "Category is required"))
        else
          val category = categoryService.getOrCreate(normalizedCategory)
          val input = TransactionInput(row.date, row.amount, category.id, row.description, row.transactionType)
          transactionService
            .validate(input)
            .left
            .map(error => IOError.ReadError("", error.toString))
            .map(_ => validInputs :+ input)
      }
    }

  private def buildExportRows: Either[IOError, Seq[TransactionCsvRow]] =
    val categoriesById = categoryService.getAll.map(category => category.id -> category.name).toMap

    transactionRepository.findAll().foldLeft[Either[IOError, Seq[TransactionCsvRow]]](Right(Seq.empty)) { (acc, transaction) =>
      acc.flatMap { rows =>
        categoriesById
          .get(transaction.category)
          .toRight(IOError.WriteError("", s"Category ${transaction.category.value} not found"))
          .map { categoryName =>
            rows :+ TransactionCsvRow(
              transaction.date,
              transaction.amount,
              categoryName,
              transaction.description,
              transaction.transactionType
            )
          }
      }
    }
