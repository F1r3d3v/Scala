package com.financemanager.infrastructure.jdbc

import com.financemanager.domain.model.{Category, CategoryId}
import com.financemanager.domain.repository.CategoryRepository
import scala.util.Using

class SqliteCategoryRepository(dbManager: JdbcDatabaseManager) extends CategoryRepository:

  private var cache: Map[CategoryId, Category] = Map.empty

  dbManager.ensureCategoriesSeeded()
  refreshCache()

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
    }.getOrElse(Map.empty)

  private def refreshCache(): Unit =
    cache = loadCache()
    if cache.isEmpty then
      dbManager.ensureCategoriesSeeded()
      cache = loadCache()

  override def findAll(): Seq[Category] =
    if cache.isEmpty then refreshCache()
    cache.values.toSeq.sortBy(_.name)

  override def add(category: String): Category =
    val result = Using(dbManager.getConnection) { conn =>
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
        else throw new Exception("Failed to insert category")
      }
    }.get
    refreshCache()
    notifyListeners()
    result

  override def remove(id: CategoryId): Unit =
    Using(dbManager.getConnection) { conn =>
      Using.resource(conn.prepareStatement("DELETE FROM categories WHERE id = ?")) { stmt =>
        stmt.setLong(1, id.value)
        stmt.executeUpdate()
      }
    }
    refreshCache()
    notifyListeners()

  def findById(id: CategoryId): Option[Category] = cache.get(id)
