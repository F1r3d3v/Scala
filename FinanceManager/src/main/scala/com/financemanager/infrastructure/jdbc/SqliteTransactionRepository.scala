package com.financemanager.infrastructure.jdbc

import com.financemanager.domain.error.DomainError
import com.financemanager.domain.model.{CategoryId, Transaction, TransactionId, TransactionInput, TransactionType}
import com.financemanager.domain.repository.TransactionRepository
import java.sql.Statement
import java.time.LocalDate
import scala.util.Using

class SqliteTransactionRepository(dbManager: JdbcDatabaseManager) extends TransactionRepository:

  override def findAll(): Seq[Transaction] =
    Using(dbManager.getConnection) { conn =>
      Using.resource(conn.createStatement()) { stmt =>
        val rs = stmt.executeQuery("SELECT id, date, amount, category_id, description, transaction_type FROM transactions")
        val b = collection.mutable.ListBuffer[Transaction]()
        while rs.next() do
          b += Transaction(
            id = TransactionId(rs.getLong("id")),
            date = LocalDate.parse(rs.getString("date")),
            amount = BigDecimal(rs.getDouble("amount")),
            category = CategoryId(rs.getLong("category_id")),
            description = rs.getString("description"),
            transactionType = TransactionType.valueOf(rs.getString("transaction_type"))
          )
        b.toSeq
      }
    }.getOrElse(Seq.empty)

  override def findById(id: TransactionId): Option[Transaction] =
    Using(dbManager.getConnection) { conn =>
      Using.resource(conn.prepareStatement("SELECT id, date, amount, category_id, description, transaction_type FROM transactions WHERE id = ?")) { stmt =>
        stmt.setLong(1, id.value)
        val rs = stmt.executeQuery()
        if rs.next() then
          Some(Transaction(
            id = TransactionId(rs.getLong("id")),
            date = LocalDate.parse(rs.getString("date")),
            amount = BigDecimal(rs.getDouble("amount")),
            category = CategoryId(rs.getLong("category_id")),
            description = rs.getString("description"),
            transactionType = TransactionType.valueOf(rs.getString("transaction_type"))
          ))
        else None
      }
    }.toOption.flatten

  override def add(input: TransactionInput): Transaction =
    val result = Using(dbManager.getConnection) { conn =>
      Using.resource(conn.prepareStatement(
        "INSERT INTO transactions (date, amount, category_id, description, transaction_type) VALUES (?, ?, ?, ?, ?)"
      )) { stmt =>
        stmt.setString(1, input.date.toString)
        stmt.setDouble(2, input.amount.toDouble)
        stmt.setLong(3, input.category.value)
        stmt.setString(4, input.description)
        stmt.setString(5, input.transactionType.toString)

        stmt.executeUpdate()
      }
      Using.resource(conn.createStatement()) { stmt =>
        val rs = stmt.executeQuery("SELECT last_insert_rowid()")
        if rs.next() then
          val id = rs.getLong(1)
          Transaction(TransactionId(id), input.date, input.amount, input.category, input.description, input.transactionType)
        else throw new Exception("Failed to insert transaction")
      }
    }.get
    notifyListeners()
    result

  override def replace(id: TransactionId, input: TransactionInput): Either[DomainError, Transaction] =
    findById(id) match
      case None => Left(DomainError.NotFound(id))
      case Some(_) =>
        val result = Using(dbManager.getConnection) { conn =>
          Using.resource(conn.prepareStatement(
            "UPDATE transactions SET date = ?, amount = ?, category_id = ?, description = ?, transaction_type = ? WHERE id = ?"
          )) { stmt =>
            stmt.setString(1, input.date.toString)
            stmt.setDouble(2, input.amount.toDouble)
            stmt.setLong(3, input.category.value)
            stmt.setString(4, input.description)
            stmt.setString(5, input.transactionType.toString)
            stmt.setLong(6, id.value)

            stmt.executeUpdate()
            Transaction(id, input.date, input.amount, input.category, input.description, input.transactionType)
          }
        }.toEither.left.map(e => DomainError.SystemError(e.getMessage))

        result.foreach(_ => notifyListeners())
        result

  override def remove(id: TransactionId): Either[DomainError, Unit] =
    findById(id) match
      case None => Left(DomainError.NotFound(id))
      case Some(_) =>
        val result = Using(dbManager.getConnection) { conn =>
          Using.resource(conn.prepareStatement("DELETE FROM transactions WHERE id = ?")) { stmt =>
            stmt.setLong(1, id.value)
            stmt.executeUpdate()
            ()
          }
        }.toEither.left.map(e => DomainError.SystemError(e.getMessage))

        result.foreach(_ => notifyListeners())
        result
