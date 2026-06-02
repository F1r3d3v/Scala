package com.financemanager.domain.service

import com.financemanager.domain.error.IOError
import com.financemanager.traits.{Loader, Writer}
import com.financemanager.domain.model.{Transaction, TransactionInput}
import com.financemanager.domain.repository.TransactionRepository

final class ImportExportService(
  transactionRepository: TransactionRepository,
  transactionService: TransactionService
):

  def importTransactions(importer: Loader[Seq[TransactionInput]]): Either[IOError, Seq[Transaction]] =
    importer.load().flatMap { inputs =>
      validateImportedTransactions(inputs).map { validInputs =>
        transactionRepository.removeAll()
        validInputs.map(transactionRepository.add)
      }
    }

  def exportTransactions(exporter: Writer[Seq[TransactionInput]]): Either[IOError, Unit] =
    val inputs = transactionRepository.findAll().map(toInput)
    exporter.write(inputs)

  private def validateImportedTransactions(
    inputs: Seq[TransactionInput]
  ): Either[IOError, Seq[TransactionInput]] =
    inputs.foldLeft[Either[IOError, Seq[TransactionInput]]](Right(Seq.empty)) { (acc, input) =>
      acc.flatMap { validInputs =>
        transactionService
          .validate(input)
          .left
          .map(error => IOError(error.toString))
          .map(_ => validInputs :+ input)
      }
    }

  private def toInput(transaction: Transaction): TransactionInput =
    TransactionInput(
      transaction.date,
      transaction.amount,
      transaction.category,
      transaction.description,
      transaction.transactionType
    )
