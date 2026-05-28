package com.financemanager.domain.service

import com.financemanager.domain.error.IOError
import com.financemanager.traits.{Loader, Writer}
import com.financemanager.domain.model.{Transaction, TransactionInput}
import com.financemanager.domain.repository.TransactionRepository

final class ImportExportService(transactionRepository: TransactionRepository):

  def importTransactions(importer: Loader[Seq[TransactionInput]]): Either[IOError, Seq[Transaction]] = {
    transactionRepository.removeAll()
    importer.load().map(_.map(transactionRepository.add))
  }

  def exportTransactions(exporter: Writer[Seq[TransactionInput]]): Either[IOError, Unit] =
    val inputs = transactionRepository.findAll().map(toInput)
    exporter.write(inputs)

  private def toInput(transaction: Transaction): TransactionInput =
    TransactionInput(
      transaction.date,
      transaction.amount,
      transaction.category,
      transaction.description,
      transaction.transactionType
    )
