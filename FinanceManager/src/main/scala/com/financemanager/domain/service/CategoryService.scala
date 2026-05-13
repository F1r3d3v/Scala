package com.financemanager.domain.service

import com.financemanager.domain.model.{Category, CategoryId}
import com.financemanager.domain.repository.CategoryRepository

/**
 * Application service for reading and maintaining categories.
 *
 * @param repository backing repository used for persistence and notifications
 */
final class CategoryService(repository: CategoryRepository):

  def getAll: Seq[Category] = repository.findAll()

  def add(category: String): Unit =
    val trimmedName = category.trim
    if trimmedName.nonEmpty then repository.add(trimmedName)

  def remove(id: CategoryId): Unit =
    repository.remove(id)
