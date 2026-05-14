package com.financemanager.domain.model

/**
 * Unique identifier for a category.
 */
opaque type CategoryId = Long

object CategoryId:
  def apply(value: Long): CategoryId = value
  extension (id: CategoryId)
    def value: Long = id

/**
 * User-defined category used to label transactions.
 *
 * @param id unique category identifier
 * @param name display name shown in the UI
 */
final case class Category(
    id: CategoryId,
    name: String
)

