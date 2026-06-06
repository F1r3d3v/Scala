package com.financemanager.infrastructure.slick
//import slick.jdbc.SQLiteProfile.api._
import com.financemanager.domain.model._
import java.time.LocalDate

/**
 * Defines the Slick table schemas and implicit column mappers for custom domain types.
 */
object SlickTables:
  import slick.jdbc.SQLiteProfile.api.{MappedColumnType, BaseColumnType, longColumnType, stringColumnType}

  implicit val categoryIdMapper: BaseColumnType[CategoryId] =
    MappedColumnType.base[CategoryId, Long]((id: CategoryId) => id.value, CategoryId.apply)

  implicit val transactionIdMapper: BaseColumnType[TransactionId] =
    MappedColumnType.base[TransactionId, Long]((id: TransactionId) => id.value, TransactionId.apply)

  implicit val transactionTypeMapper: BaseColumnType[TransactionType] =
    MappedColumnType.base[TransactionType, String](_.toString, TransactionType.valueOf)

  implicit val localDateMapper: BaseColumnType[LocalDate] =
    MappedColumnType.base[LocalDate, String](_.toString, LocalDate.parse)

  implicit val bigDecimalMapper: BaseColumnType[BigDecimal] =
    MappedColumnType.base[BigDecimal, String](_.toString, BigDecimal.apply)

  import slick.jdbc.SQLiteProfile.api._

  class CategoriesTable(tag: Tag) extends Table[Category](tag, "categories"):
    def id = column[CategoryId]("id", O.PrimaryKey, O.AutoInc)
    def name = column[String]("name", O.Unique)
    def * = (id, name).mapTo[Category]

  val categories = TableQuery[CategoriesTable]

  class TransactionsTable(tag: Tag) extends Table[Transaction](tag, "transactions"):
    def id = column[TransactionId]("id", O.PrimaryKey, O.AutoInc)
    def date = column[LocalDate]("date")(using localDateMapper)
    def amount = column[BigDecimal]("amount")(using bigDecimalMapper)
    def categoryId = column[CategoryId]("category_id")
    def description = column[String]("description")
    def transactionType = column[TransactionType]("transaction_type")

    def categoryFk = foreignKey("cat_fk", categoryId, categories)(_.id)

    def * = (id, date, amount, categoryId, description, transactionType).mapTo[Transaction]

  val transactions = TableQuery[TransactionsTable]

