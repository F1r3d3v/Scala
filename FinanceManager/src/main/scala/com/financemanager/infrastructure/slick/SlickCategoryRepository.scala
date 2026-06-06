package com.financemanager.infrastructure.slick

import com.financemanager.domain.model.{Category, CategoryId}
import com.financemanager.domain.repository.CategoryRepository
import slick.jdbc.SQLiteProfile.api._
import scala.concurrent.Await
import scala.concurrent.duration.Duration

class SlickCategoryRepository(dbManager: SlickDatabaseManager) extends CategoryRepository:
  import SlickTables._

  private val db = dbManager.db
  private var cache: Map[CategoryId, Category] = Map.empty

  refreshCache()

  private def loadCache(): Map[CategoryId, Category] =
    val query = categories.result
    val resultSeq = Await.result(db.run(query), Duration.Inf)
    resultSeq.map(cat => cat.id -> cat).toMap

  private def refreshCache(): Unit =
    cache = loadCache()

  override def findAll(): Seq[Category] =
    if cache.isEmpty then refreshCache()
    cache.values.toSeq.sortBy(_.name)

  override def add(categoryName: String): Category =
    val insertQuery = (categories.map(_.name) returning categories.map(_.id)
      into ((name, id) => Category(id, name)))
    
    val existingIdOpt = Await.result(db.run(categories.filter(_.name === categoryName).map(_.id).result.headOption), Duration.Inf)

    val newCategory = existingIdOpt match
      case Some(id) => Category(id, categoryName)
      case None =>
        Await.result(db.run(insertQuery += categoryName), Duration.Inf)

    refreshCache()
    notifyListeners()
    newCategory

  override def remove(id: CategoryId): Unit =
    val deleteAction = categories.filter(_.id === id).delete
    Await.result(db.run(deleteAction), Duration.Inf)
    refreshCache()
    notifyListeners()

  def findById(id: CategoryId): Option[Category] = cache.get(id)