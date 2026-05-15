package com.financemanager.domain.repository

import com.financemanager.domain.model.{Category, CategoryId}

/**
 * In-memory repository for categories, used in tests.
 */
final class InMemoryCategoryRepository(initialCategories: Seq[Category] = Seq.empty) extends CategoryRepository:
  private var categories: Vector[Category] = initialCategories.toVector
  private var nextRawId: Long = initialCategories.map(_.id.value).maxOption.getOrElse(0L) + 1

  override def findAll(): Seq[Category] = categories

  override def add(category: String): Category =
    val c = Category(CategoryId(nextRawId), category)
    nextRawId += 1
    categories = categories :+ c
    notifyListeners()
    c

  override def remove(id: CategoryId): Unit =
    categories = categories.filterNot(c => c.id == id)
    notifyListeners()

