package com.financemanager.infrastructure.jdbc

import munit.FunSuite
import java.nio.file.Files
import java.sql.Connection
import scala.util.Using
import com.financemanager.config.AppConfig

class JdbcDatabaseManagerTest extends FunSuite:

  private def withTempDb(testCode: JdbcDatabaseManager => Unit): Unit =
    val tempFile = Files.createTempFile("financemanager_test", ".db")
    val dbUrl = s"jdbc:sqlite:${tempFile.toAbsolutePath.toString}"
    val manager = JdbcDatabaseManager(dbUrl)
    try
      testCode(manager)
    finally
      Files.deleteIfExists(tempFile)

  test(
    "initializeSchema should create both categories and transactions tables"
  ):
    withTempDb { manager =>
      manager.initializeSchema()
      Using(manager.getConnection) { conn =>
        Using.resource(conn.createStatement()) { stmt =>
          val rs = stmt.executeQuery(
            "SELECT name FROM sqlite_master WHERE type='table' ORDER BY name;"
          )
          val tables = scala.collection.mutable.ListBuffer[String]()
          while rs.next() do tables += rs.getString(1)

          assert(
            tables.contains("categories"),
            "The categories table should be present"
          )
          assert(
            tables.contains("transactions"),
            "The transactions table should be present"
          )
        }
      }
    }

  test(
    "ensureCategoriesSeeded should insert the predefined initial categories when the table is empty"
  ):
    withTempDb { manager =>
      manager.initializeSchema()
      manager.ensureCategoriesSeeded()

      Using(manager.getConnection) { conn =>
        Using.resource(conn.createStatement()) { stmt =>
          val rs =
            stmt.executeQuery("SELECT name FROM categories ORDER BY name;")
          val categories = scala.collection.mutable.ListBuffer[String]()
          while rs.next() do categories += rs.getString(1)

          val expected = AppConfig.InitialCategories.sorted
          assertEquals(
            categories.toList,
            expected.toList,
            "The initial categories must be seeded successfully"
          )
        }
      }
    }

  test(
    "ensureCategoriesSeeded should not duplicate categories if called multiple times"
  ):
    withTempDb { manager =>
      manager.initializeSchema()
      manager.ensureCategoriesSeeded()
      manager.ensureCategoriesSeeded()

      Using(manager.getConnection) { conn =>
        Using.resource(conn.createStatement()) { stmt =>
          val rs = stmt.executeQuery("SELECT COUNT(*) FROM categories;")
          assert(rs.next(), "Result set should have a row")
          assertEquals(
            rs.getInt(1),
            AppConfig.InitialCategories.size,
            "There should be exactly the initial number of categories in DB without duplicates"
          )
        }
      }
    }

  test(
    "ensureCategoriesSeeded should avoid seeding if categories already exist (even custom ones)"
  ):
    withTempDb { manager =>
      manager.initializeSchema()
      Using(manager.getConnection) { conn =>
        Using.resource(
          conn.prepareStatement("INSERT INTO categories (name) VALUES (?)")
        ) { stmt =>
          stmt.setString(1, "CustomCategory")
          stmt.executeUpdate()
        }
      }

      manager.ensureCategoriesSeeded()

      Using(manager.getConnection) { conn =>
        Using.resource(conn.createStatement()) { stmt =>
          val rs = stmt.executeQuery("SELECT name FROM categories;")
          val categories = scala.collection.mutable.ListBuffer[String]()
          while rs.next() do categories += rs.getString(1)

          assertEquals(
            categories.toList,
            List("CustomCategory"),
            "No categories should be seeded when the table is already populated"
          )
        }
      }
    }
