package com.financemanager.infrastructure.jdbc

import java.sql.{Connection, DriverManager}
import scala.util.Using
import com.financemanager.config.AppConfig

/** Manages the SQLite database connection and schema initialization.
  *
  * Provides methods for safely acquiring connections and ensuring that the
  * required core tables and initial configuration seed values are established.
  *
  * @param dbUrl
  *   JDBC URL for the backend database
  */
class JdbcDatabaseManager(dbUrl: String):

  Class.forName("org.sqlite.JDBC")

  /** Acquires a new database connection from the driver.
    *
    * @return
    *   active java.sql.Connection
    */
  def getConnection: Connection =
    val conn = DriverManager.getConnection(dbUrl)
    Using.resource(conn.createStatement()) { stmt =>
      stmt.execute("PRAGMA foreign_keys = ON")
    }
    conn

  /** Initializes the core table structures required by the application.
    *
    * Idempotent method that ensures `categories` and `transactions` tables
    * exist in the target database.
    */
  def initializeSchema(): Unit =
    Using(getConnection) { conn =>
      Using.resource(conn.createStatement()) { stmt =>
        val createCategoriesTable = """
          CREATE TABLE IF NOT EXISTS categories (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            name TEXT NOT NULL UNIQUE
          );
        """
        stmt.execute(createCategoriesTable)

        val createTransactionsTable = """
          CREATE TABLE IF NOT EXISTS transactions (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            date TEXT NOT NULL,
            amount REAL NOT NULL,
            category_id INTEGER NOT NULL,
            description TEXT NOT NULL,
            transaction_type TEXT NOT NULL,
            FOREIGN KEY(category_id) REFERENCES categories(id)
          );
        """
        stmt.execute(createTransactionsTable)
      }
    }

  /** Validates the categories table and seeds missing starting values.
    *
    * Ensures the database has a working set of classification names to rely
    * upon if the table is completely empty.
    */
  def ensureCategoriesSeeded(): Unit =
    val categories = AppConfig.InitialCategories
    Using(getConnection) { conn =>
      Using.resource(conn.createStatement()) { stmt =>
        val rs = stmt.executeQuery("SELECT COUNT(*) FROM categories")
        val isEmpty = rs.next() && rs.getInt(1) == 0
        if isEmpty then
          val originalAutoCommit = conn.getAutoCommit
          conn.setAutoCommit(false)
          try
            Using.resource(
              conn.prepareStatement(
                "INSERT OR IGNORE INTO categories (name) VALUES (?)"
              )
            ) { insertStmt =>
              categories.foreach { name =>
                insertStmt.setString(1, name)
                insertStmt.executeUpdate()
              }
            }
            conn.commit()
          finally conn.setAutoCommit(originalAutoCommit)
      }
    }
