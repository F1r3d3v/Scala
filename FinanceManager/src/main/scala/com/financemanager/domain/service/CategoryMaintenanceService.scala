package com.financemanager.domain.service

import com.financemanager.domain.model.{Category, CategoryId}
import com.financemanager.domain.repository.TransactionRepository

/**
 * Summary used by the UI before deleting a category.
 *
 * @param category category selected for deletion
 * @param assignedTransactionCount number of transactions currently using the category
 * @param replacementCategoryName fallback category used when transactions must be preserved
 */
final case class CategoryDeletionPreview(
    category: Category,
    assignedTransactionCount: Int,
    replacementCategoryName: String
)

/**
 * Coordinates category deletion with transaction reassignment rules.
 *
 * @param categoryService source of categories and creation/removal operations
 * @param transactionRepository repository used to inspect and reassign transactions
 */
final class CategoryMaintenanceService(
  categoryService: CategoryService,
  transactionRepository: TransactionRepository
):
  private val UnknownCategoryName = "Unknown"

  /**
   * Builds a deletion preview including the number of affected transactions.
   */
  def previewDeletion(id: CategoryId): Option[CategoryDeletionPreview] =
    categoryService.findById(id).map { category =>
      CategoryDeletionPreview(
        category = category,
        assignedTransactionCount = transactionRepository.countByCategory(id),
        replacementCategoryName = UnknownCategoryName
      )
    }

  /**
   * Deletes the category and reassigns dependent transactions to `Unknown` when needed.
   */
  def deleteCategory(id: CategoryId): Either[String, Unit] =
    previewDeletion(id).toRight("Category not found").flatMap { preview =>
      if preview.category.name.equalsIgnoreCase(UnknownCategoryName) then
        Left("The Unknown category is reserved and cannot be deleted")
      else
        if preview.assignedTransactionCount > 0 then
          val unknownCategory = categoryService.getOrCreate(UnknownCategoryName)
          transactionRepository.reassignCategory(preview.category.id, unknownCategory.id)

        categoryService.remove(preview.category.id)
        Right(())
    }
