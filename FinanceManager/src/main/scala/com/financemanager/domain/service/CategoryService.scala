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

  def subscribe(listener: () => Unit): Unit =
    repository.subscribe(listener)

  def findByName(name: String): Option[Category] =
    val normalized = name.trim
    if normalized.isEmpty then None
    else repository.findAll().find(_.name.equalsIgnoreCase(normalized))

  def findById(id: CategoryId): Option[Category] =
    repository.findAll().find(_.id == id)

  def add(category: String): Unit =
    val trimmedName = category.trim
    if trimmedName.nonEmpty then repository.add(trimmedName)

  def getOrCreate(category: String): Category =
    val trimmedName = category.trim
    require(trimmedName.nonEmpty, "Category name cannot be empty")
    findByName(trimmedName).getOrElse(repository.add(trimmedName))

  def remove(id: CategoryId): Unit =
    repository.remove(id)
