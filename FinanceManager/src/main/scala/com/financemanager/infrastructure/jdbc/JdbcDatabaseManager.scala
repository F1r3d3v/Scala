package com.financemanager.infrastructure.jdbc

import java.sql.{Connection, DriverManager}
import java.time.LocalDate
import scala.util.Using
import com.financemanager.config.AppConfig

class JdbcDatabaseManager(dbUrl: String):

  Class.forName("org.sqlite.JDBC")

  def getConnection: Connection =
    DriverManager.getConnection(dbUrl)

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

  def seedInitialCategories(): Unit =
    val categories = AppConfig.InitialCategories
    Using(getConnection) { conn =>
      Using.resource(conn.createStatement()) { stmt =>
        val rs = stmt.executeQuery("SELECT COUNT(*) FROM categories")
        val isEmpty = rs.next() && rs.getInt(1) == 0
        if isEmpty then
          Using.resource(conn.prepareStatement("INSERT OR IGNORE INTO categories (name) VALUES (?)")) { insertStmt =>
            categories.foreach { name =>
              insertStmt.setString(1, name)
              insertStmt.executeUpdate()
            }
          }
      }
    }

  def ensureCategoriesSeeded(): Unit =
    val categories = AppConfig.InitialCategories
    Using(getConnection) { conn =>
      Using.resource(conn.createStatement()) { stmt =>
        val rs = stmt.executeQuery("SELECT COUNT(*) FROM categories")
        val isEmpty = rs.next() && rs.getInt(1) == 0
        if isEmpty then
          Using.resource(conn.prepareStatement("INSERT OR IGNORE INTO categories (name) VALUES (?)")) { insertStmt =>
            categories.foreach { name =>
              insertStmt.setString(1, name)
              insertStmt.executeUpdate()
            }
          }
      }
    }

