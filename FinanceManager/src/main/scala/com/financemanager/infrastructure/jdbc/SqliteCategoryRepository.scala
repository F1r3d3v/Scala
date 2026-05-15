package com.financemanager.infrastructure.jdbc

import com.financemanager.domain.model.{Category, CategoryId}
import com.financemanager.domain.repository.CategoryRepository
import scala.util.Using

/**
 * SQLite-backed implementation of the CategoryRepository.
 *
 * Manages category persistence and fetching from a relational database,
 * utilizing an internal cache to reduce redundant DB reads.
 *
 * @param dbManager database manager handling connections and setup
 */
class SqliteCategoryRepository(dbManager: JdbcDatabaseManager) extends CategoryRepository:

  private var cache: Map[CategoryId, Category] = Map.empty

  dbManager.ensureCategoriesSeeded()
  refreshCache()

  /**
   * Retrieves all categories presently in the database.
   *
   * @return a map consisting of Category ID as a key and Category data as a value
   */
  private def loadCache(): Map[CategoryId, Category] =
    Using(dbManager.getConnection) { conn =>
      Using.resource(conn.createStatement()) { stmt =>
        val rs = stmt.executeQuery("SELECT id, name FROM categories")
        val b = collection.mutable.Map[CategoryId, Category]()
        while rs.next() do
          val id = CategoryId(rs.getLong("id"))
          val name = rs.getString("name")
          b.put(id, Category(id, name))
        b.toMap
      }
    } match
      case scala.util.Success(map) => map
      case scala.util.Failure(err) =>
        System.err.println(s"Failed to load categories: ${err.getMessage}")
        Map.empty

  /**
   * Reloads internal cached data from the database.
   * If retrieved cache is empty, enforces initial categories configuration.
   */
  private def refreshCache(): Unit =
    cache = loadCache()
    if cache.isEmpty then
      dbManager.ensureCategoriesSeeded()
      cache = loadCache()

  /**
   * Retrieves all configured categories, fetching from DB if cache has not been populated.
   *
   * @return sorted sequence of available `Category` items
   */
  override def findAll(): Seq[Category] =
    if cache.isEmpty then refreshCache()
    cache.values.toSeq.sortBy(_.name)

  /**
   * Persists a new category matching the specified name into the repository.
   *
   * @param category textual name assigned to the new category
   * @return generated Category model instance including assigned unique identifier
   */
  override def add(category: String): Category =
    val result = Using(dbManager.getConnection) { conn =>
      val existingId = Using.resource(conn.prepareStatement("SELECT id FROM categories WHERE name = ?")) { stmt =>
        stmt.setString(1, category)
        val rs = stmt.executeQuery()
        if rs.next() then Some(rs.getLong("id")) else None
      }

      existingId match
        case Some(id) => Category(CategoryId(id), category)
        case None =>
          Using.resource(conn.prepareStatement(
            "INSERT INTO categories (name) VALUES (?)"
          )) { stmt =>
            stmt.setString(1, category)
            stmt.executeUpdate()
          }
          Using.resource(conn.createStatement()) { stmt =>
            val rs = stmt.executeQuery("SELECT last_insert_rowid()")
            if rs.next() then
              Category(CategoryId(rs.getLong(1)), category)
            else throw new IllegalStateException("Failed to insert category")
          }
    }.get
    refreshCache()
    notifyListeners()
    result

  /**
   * Drops category with matching identifier from the persistent storage.
   *
   * @param id identifier of targeted category
   */
  override def remove(id: CategoryId): Unit =
    Using(dbManager.getConnection) { conn =>
      Using.resource(conn.prepareStatement("DELETE FROM categories WHERE id = ?")) { stmt =>
        stmt.setLong(1, id.value)
        stmt.executeUpdate()
      }
    }
    refreshCache()
    notifyListeners()

  /**
   * Examines cache for a category identified by the specified ID.
   *
   * @param id numeric category identifier
   * @return optional Category instance if tracked
   */
  def findById(id: CategoryId): Option[Category] = cache.get(id)
