package com.financemanager.infrastructure.slick

import munit.FunSuite
import java.nio.file.Files
import java.sql.DriverManager
import scala.util.Using
import com.financemanager.config.AppConfig

class SlickDatabaseManagerTest extends FunSuite:
  /**
   * Helper fixture that provisions a temporary SQLite database, instantiates
   * the Slick manager, and provides a raw JDBC URL for independent assertions.
   */
  private def withTempDb(testCode: (SlickDatabaseManager, String) => Unit): Unit =
    val tempFile = Files.createTempFile("financemanager_slick_test", ".db")
    val dbUrl = s"jdbc:sqlite:${tempFile.toAbsolutePath.toString}"
    val manager = new SlickDatabaseManager(dbUrl)
    try
      testCode(manager, dbUrl)
    finally
      manager.db.close()
      Files.deleteIfExists(tempFile)

  test("initializeSchema should create both categories and transactions tables"):
    withTempDb { (manager, dbUrl) =>
      manager.initializeSchema()

      Using.resource(DriverManager.getConnection(dbUrl)) { conn =>
        Using.resource(conn.createStatement()) { stmt =>
          val rs = stmt.executeQuery("SELECT name FROM sqlite_master WHERE type='table' ORDER BY name;")
          val tables = scala.collection.mutable.ListBuffer[String]()
          while rs.next() do tables += rs.getString(1)

          assert(tables.contains("categories"), "The categories table should be present")
          assert(tables.contains("transactions"), "The transactions table should be present")
        }
      }
    }

  test("initializeSchema should insert the predefined initial categories when the table is empty"):
    withTempDb { (manager, dbUrl) =>
      manager.initializeSchema()

      Using.resource(DriverManager.getConnection(dbUrl)) { conn =>
        Using.resource(conn.createStatement()) { stmt =>
          val rs = stmt.executeQuery("SELECT name FROM categories ORDER BY name;")
          val categories = scala.collection.mutable.ListBuffer[String]()
          while rs.next() do categories += rs.getString(1)

          val expected = AppConfig.InitialCategories.sorted
          assertEquals(categories.toList, expected.toList, "The initial categories must be seeded successfully")
        }
      }
    }

  test("initializeSchema should be idempotent and not duplicate categories if called multiple times"):
    withTempDb { (manager, dbUrl) =>
      manager.initializeSchema()
      manager.initializeSchema()

      Using.resource(DriverManager.getConnection(dbUrl)) { conn =>
        Using.resource(conn.createStatement()) { stmt =>
          val rs = stmt.executeQuery("SELECT COUNT(*) FROM categories;")
          assert(rs.next(), "Result set should have a row")
          assertEquals(rs.getInt(1), AppConfig.InitialCategories.size, "There should be exactly the initial number of categories in DB without duplicates")
        }
      }
    }

  test("initializeSchema should avoid seeding if categories already exist (even custom ones)"):
    withTempDb { (manager, dbUrl) =>
      manager.initializeSchema()
      
      Using.resource(DriverManager.getConnection(dbUrl)) { conn =>
        Using.resource(conn.createStatement()) { stmt =>
          stmt.executeUpdate("DELETE FROM categories;")
          stmt.executeUpdate("INSERT INTO categories (name) VALUES ('CustomCategory');")
        }
      }
      
      manager.initializeSchema()

      Using.resource(DriverManager.getConnection(dbUrl)) { conn =>
        Using.resource(conn.createStatement()) { stmt =>
          val rs = stmt.executeQuery("SELECT name FROM categories;")
          val categories = scala.collection.mutable.ListBuffer[String]()
          while rs.next() do categories += rs.getString(1)

          assertEquals(categories.toList, List("CustomCategory"), "No categories should be seeded when the table is already populated")
        }
      }
    }