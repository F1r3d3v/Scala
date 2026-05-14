package com.financemanager.domain.repository

import com.financemanager.domain.model.{Category, CategoryId}

/**
 * Repository contract for CRUD operations on categories.
 */
trait CategoryRepository extends Subscribable:
  def findAll(): Seq[Category]
  def add(category: String): Category
  def remove(id: CategoryId): Unit
