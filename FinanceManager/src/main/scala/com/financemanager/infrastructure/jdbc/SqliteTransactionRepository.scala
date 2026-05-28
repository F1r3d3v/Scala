package com.financemanager.infrastructure.jdbc

import com.financemanager.domain.error.DomainError
import com.financemanager.domain.model.{CategoryId, Transaction, TransactionId, TransactionInput, TransactionType}
import com.financemanager.domain.repository.TransactionRepository
import java.time.LocalDate
import scala.util.Using

/**
 * SQLite-backed implementation of the TransactionRepository.
 *
 * Defines standard operations on budget records persisting
 * to an underlying JDBC SQLite database.
 *
 * @param dbManager database connection provider
 */
class SqliteTransactionRepository(dbManager: JdbcDatabaseManager) extends TransactionRepository:

  /**
   * Retrieves all historical transaction records stored in the database.
   *
   * @return sequence containing parsed and available transactions
   */
  override def findAll(): Seq[Transaction] =
    Using(dbManager.getConnection) { conn =>
      Using.resource(conn.prepareStatement("SELECT id, date, CAST(amount AS TEXT) as amount, category_id, description, transaction_type FROM transactions")) { stmt =>
        val rs = stmt.executeQuery()
        val b = collection.mutable.ListBuffer[Transaction]()
        while rs.next() do
          b += Transaction(
            id = TransactionId(rs.getLong("id")),
            date = LocalDate.parse(rs.getString("date")),
            amount = BigDecimal(rs.getString("amount")),
            category = CategoryId(rs.getLong("category_id")),
            description = rs.getString("description"),
            transactionType = TransactionType.valueOf(rs.getString("transaction_type"))
          )
        b.toSeq
      }
    } match
      case scala.util.Success(seq) => seq
      case scala.util.Failure(err) =>
        System.err.println(s"Failed to load transactions: ${err.getMessage}")
        Seq.empty

  /**
   * Attempts resolving solitary transaction from the database by identifier.
   *
   * @param id wrapper for unique transaction reference
   * @return option with Transaction match when successful
   */
  override def findById(id: TransactionId): Option[Transaction] =
    Using(dbManager.getConnection) { conn =>
      Using.resource(conn.prepareStatement("SELECT id, date, CAST(amount AS TEXT) as amount, category_id, description, transaction_type FROM transactions WHERE id = ?")) { stmt =>
        stmt.setLong(1, id.value)
        val rs = stmt.executeQuery()
        if rs.next() then
          Some(Transaction(
            id = TransactionId(rs.getLong("id")),
            date = LocalDate.parse(rs.getString("date")),
            amount = BigDecimal(rs.getString("amount")),
            category = CategoryId(rs.getLong("category_id")),
            description = rs.getString("description"),
            transactionType = TransactionType.valueOf(rs.getString("transaction_type"))
          ))
        else None
      }
    } match
      case scala.util.Success(opt) => opt
      case scala.util.Failure(err) =>
        System.err.println(s"Failed to load transaction ${id.value}: ${err.getMessage}")
        None

  /**
   * Translates incoming domain data object into a new persisted record.
   * Dispatches notifications to subscribers upon structural database update.
   *
   * @param input newly registered payload for an expense or income
   * @return established and fully identified transaction record
   */
  override def add(input: TransactionInput): Transaction =
    val result = Using(dbManager.getConnection) { conn =>
      Using.resource(conn.prepareStatement(
        "INSERT INTO transactions (date, amount, category_id, description, transaction_type) VALUES (?, ?, ?, ?, ?)"
      )) { stmt =>
        stmt.setString(1, input.date.toString)
        stmt.setString(2, input.amount.toString)
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
        else throw new IllegalStateException("Failed to insert transaction")
      }
    }.get
    notifyListeners()
    result

  /**
   * Rewrites an existing targeted transaction given its specific identifier.
   * Emits signal alerting dependents in the case of successful alteration.
   *
   * @param id domain ID targeting the modified data element
   * @param input revised transactional data mapping
   * @return a domain entity on success or matching error type if ID cannot be correlated
   */
  override def replace(id: TransactionId, input: TransactionInput): Either[DomainError, Transaction] =
    findById(id) match
      case None => Left(DomainError.NotFound(id))
      case Some(_) =>
        val result = Using(dbManager.getConnection) { conn =>
          Using.resource(conn.prepareStatement(
            "UPDATE transactions SET date = ?, amount = ?, category_id = ?, description = ?, transaction_type = ? WHERE id = ?"
          )) { stmt =>
            stmt.setString(1, input.date.toString)
            stmt.setString(2, input.amount.toString)
            stmt.setLong(3, input.category.value)
            stmt.setString(4, input.description)
            stmt.setString(5, input.transactionType.toString)
            stmt.setLong(6, id.value)

            val updatedRows = stmt.executeUpdate()
            if updatedRows == 0 then Left(DomainError.NotFound(id))
            else Right(Transaction(id, input.date, input.amount, input.category, input.description, input.transactionType))
          }
        }.toEither
          .left.map(e => DomainError.SystemError(e.getMessage))
          .flatMap(identity)

        result.foreach(_ => notifyListeners())
        result

  /**
   * Purges designated transactional record from underlying permanent storage.
   * Publishes notification allowing synchronous GUI refreshing after elimination.
   *
   * @param id logical index linked to item selected for deletion
   * @return successful completion Unit, or NotFound failure
   */
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

  /**
   * Executes complete clearance of all transaction records from the database.
   * Notifies dependents to trigger necessary UI updates after the purge.
   */
  override def removeAll(): Unit =
    Using(dbManager.getConnection) { conn =>
      Using.resource(conn.prepareStatement("DELETE FROM transactions")) { stmt =>
        stmt.executeUpdate()
      }
    } match
      case scala.util.Success(_) => notifyListeners()
      case scala.util.Failure(err) =>
        System.err.println(s"Failed to clear transactions: ${err.getMessage}")
