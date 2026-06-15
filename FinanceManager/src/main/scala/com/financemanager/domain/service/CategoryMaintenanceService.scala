package com.financemanager.domain.service

import com.financemanager.domain.model.{Category, CategoryId}
import com.financemanager.domain.repository.TransactionRepository

final case class CategoryDeletionPreview(
    category: Category,
    assignedTransactionCount: Int,
    replacementCategoryName: String
)

final class CategoryMaintenanceService(
  categoryService: CategoryService,
  transactionRepository: TransactionRepository
):
  private val UnknownCategoryName = "Unknown"

  def previewDeletion(id: CategoryId): Option[CategoryDeletionPreview] =
    categoryService.findById(id).map { category =>
      CategoryDeletionPreview(
        category = category,
        assignedTransactionCount = transactionRepository.findAll().count(_.category == id),
        replacementCategoryName = UnknownCategoryName
      )
    }

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
