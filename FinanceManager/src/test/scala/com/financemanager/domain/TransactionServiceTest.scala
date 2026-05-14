package com.financemanager.domain

import com.financemanager.domain.error.DomainError
import com.financemanager.domain.model.{TransactionId, TransactionInput, TransactionType, CategoryId}
import com.financemanager.domain.service.TransactionService
import com.financemanager.testutil.TestRepository
import java.time.LocalDate
import munit.FunSuite

/**
 * Tests for transaction validation and CRUD behavior.
 */
class TransactionServiceTest extends FunSuite:

  private val validInput = TransactionInput(
    date = LocalDate.now(),
    amount = BigDecimal("50.00"),
    category = CategoryId(1L),
    description = "Weekly shopping",
    transactionType = TransactionType.Expense
  )

  test("add valid transaction succeeds"):
    val repo = TestRepository()
    val service = TransactionService(repo)
    val result = service.add(validInput)
    assert(result.isRight, clue = "expected valid input to be accepted")
    assertEquals(result.toOption.get.amount, BigDecimal("50.00"), clue = "expected the stored amount to match the input")
    assertEquals(repo.findAll().size, 1, clue = "expected one transaction in the repository")

  test("add with zero amount returns InvalidAmount"):
    val service = TransactionService(TestRepository())
    val input = validInput.copy(amount = BigDecimal(0))
    val result = service.add(input)
    assert(result.isLeft, clue = "expected zero amount to be rejected")
    assert(clue(result.left.toOption).exists(_.isInstanceOf[DomainError.InvalidAmount]), clue = "expected InvalidAmount error")

  test("add with negative amount returns InvalidAmount"):
    val service = TransactionService(TestRepository())
    val input = validInput.copy(amount = BigDecimal("-10.00"))
    assert(service.add(input).isLeft, clue = "expected negative amount to be rejected")

  test("add with empty category returns InvalidCategory"):
    val service = TransactionService(TestRepository())
    val input = validInput.copy(category = CategoryId(0L))
    val result = service.add(input)
    assert(clue(result.left.toOption).exists(_.isInstanceOf[DomainError.InvalidCategory]), clue = "expected InvalidCategory error")

  test("add with empty description returns InvalidDescription"):
    val service = TransactionService(TestRepository())
    val input = validInput.copy(description = "")
    val result = service.add(input)
    assert(clue(result.left.toOption).exists(_.isInstanceOf[DomainError.InvalidDescription]), clue = "expected InvalidDescription error")

  test("update existing transaction succeeds"):
    val repo = TestRepository()
    val service = TransactionService(repo)
    val added = service.add(validInput).toOption.get
    val updated = service.update(added.id, validInput.copy(amount = BigDecimal("75.00")))
    assert(updated.isRight, clue = "expected existing transaction to update")
    assertEquals(updated.toOption.get.amount, BigDecimal("75.00"), clue = "expected updated amount to be saved")

  test("update non-existent transaction returns NotFound"):
    val service = TransactionService(TestRepository())
    val result = service.update(TransactionId(999L), validInput)
    assert(result.isLeft, clue = "expected missing transaction to fail update")

  test("delete existing transaction succeeds"):
    val repo = TestRepository()
    val service = TransactionService(repo)
    val added = service.add(validInput).toOption.get
    assert(service.delete(added.id).isRight, clue = "expected existing transaction to delete")
    assertEquals(repo.findAll().size, 0, clue = "expected repository to be empty after delete")

  test("delete non-existent transaction returns NotFound"):
    val service = TransactionService(TestRepository())
    val result = service.delete(TransactionId(999L))
    assert(result.isLeft, clue = "expected missing transaction to fail delete")

  test("add income transaction succeeds"):
    val repo = TestRepository()
    val service = TransactionService(repo)
    val income = validInput.copy(transactionType = TransactionType.Income, category = CategoryId(2L), description = "Monthly pay")
    val result = service.add(income)
    assert(result.isRight, clue = "expected income transaction to be accepted")
    assertEquals(result.toOption.get.transactionType, TransactionType.Income, clue = "expected the transaction type to remain income")
