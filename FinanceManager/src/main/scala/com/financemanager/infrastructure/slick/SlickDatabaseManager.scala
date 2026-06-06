package com.financemanager.infrastructure.slick

import slick.jdbc.SQLiteProfile.api._
import com.financemanager.config.AppConfig
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import slick.jdbc.meta.MTable
import scala.concurrent.ExecutionContext.Implicits.global

class SlickDatabaseManager(dbUrl : String):
  private val urlWithFk = if (dbUrl.contains("?")) s"$dbUrl&foreign_keys=ON" else s"$dbUrl?foreign_keys=ON"
  val db = Database.forURL(urlWithFk, driver = "org.sqlite.JDBC")

  /**
   * Initializes schemas and handles category seeding.
   */
  def initializeSchema(): Unit =
    import SlickTables._

    val setupAction = for {
      existingTables <- MTable.getTables
      tableNames = existingTables.map(_.name.name)

      // Create schemas if they don't exist
      _ <- if (!tableNames.contains("categories")) categories.schema.create else DBIO.successful(())
      _ <- if (!tableNames.contains("transactions")) transactions.schema.create else DBIO.successful(())

      // Seed categories if empty
      count <- categories.length.result
      _ <- if (count == 0) {
        categories.map(_.name) ++= AppConfig.InitialCategories
      } else {
        DBIO.successful(())
      }
    } yield ()

    Await.result(db.run(setupAction), Duration.Inf)
