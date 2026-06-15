package com.financemanager.domain

import com.financemanager.domain.model.{Transaction, TransactionId, CategoryId}
import com.financemanager.domain.model.TransactionType
import com.financemanager.domain.service.{
  AnalyticsService,
  Granularity,
  TrendResult
}
import com.financemanager.testutil.TestRepository
import com.financemanager.domain.repository.InMemoryCategoryRepository
import com.financemanager.domain.model.Category
import java.time.LocalDate
import munit.FunSuite

/** Tests for analytics aggregation logic.
  */
class AnalyticsServiceTest extends FunSuite:

  private def nameToId(name: String): Long = name match
    case "Food"      => 1L
    case "Transport" => 2L
    case "Salary"    => 4L
    case "Small"     => 1L
    case "Big"       => 2L
    case "Medium"    => 3L
    case _           => 1L

  private def expense(
      amount: BigDecimal,
      date: LocalDate,
      categoryName: String = "Food",
      id: Long = 1
  ) =
    Transaction(
      TransactionId(id),
      date,
      amount,
      CategoryId(nameToId(categoryName)),
      "Desc",
      TransactionType.Expense
    )

  private def income(
      amount: BigDecimal,
      date: LocalDate,
      categoryId: Long = 1L,
      id: Long = 1
  ) =
    Transaction(
      TransactionId(id),
      date,
      amount,
      CategoryId(categoryId),
      "Desc",
      TransactionType.Income
    )

  test("spendingByCategory groups only expenses"):
    val now = LocalDate.now()
    val transactions = Seq(
      expense(BigDecimal("50"), now, "Food"),
      expense(BigDecimal("30"), now, "Food", 2),
      expense(BigDecimal("20"), now, "Transport", 3),
      income(BigDecimal("1000"), now, 4)
    )
    val categoryRepo = new InMemoryCategoryRepository(
      Seq(
        Category(CategoryId(1L), "Food"),
        Category(CategoryId(2L), "Transport"),
        Category(CategoryId(4L), "Salary")
      )
    )
    val result = new AnalyticsService(
      TestRepository(transactions*),
      categoryRepo
    ).spendingByCategory(now.minusDays(1), now.plusDays(1))
    assertEquals(
      result.size,
      2,
      clue = "expected only expense categories to be included"
    )
    assertEquals(
      result.find(_._1 == "Food").map(_._2),
      Some(BigDecimal("80")),
      clue = "expected Food to be aggregated"
    )
    assertEquals(
      result.find(_._1 == "Transport").map(_._2),
      Some(BigDecimal("20")),
      clue = "expected Transport to be aggregated"
    )

  test("spendingByCategory sorts by amount descending"):
    val now = LocalDate.now()
    val transactions = Seq(
      expense(BigDecimal("10"), now, "Small"),
      expense(BigDecimal("100"), now, "Big", 2),
      expense(BigDecimal("50"), now, "Medium", 3)
    )
    val categoryRepo = new InMemoryCategoryRepository(
      Seq(
        Category(CategoryId(1L), "Small"),
        Category(CategoryId(2L), "Big"),
        Category(CategoryId(3L), "Medium")
      )
    )
    val result = new AnalyticsService(
      TestRepository(transactions*),
      categoryRepo
    ).spendingByCategory(now.minusDays(1), now.plusDays(1))
    assertEquals(
      result.map(_._1),
      Seq("Big", "Medium", "Small"),
      clue = "expected categories sorted by descending amount"
    )

  test("spendingByCategory with no expenses returns empty"):
    val now = LocalDate.now()
    val categoryRepo =
      new InMemoryCategoryRepository(Seq(Category(CategoryId(1L), "Salary")))
    val result = new AnalyticsService(
      TestRepository(income(BigDecimal("1000"), now)),
      categoryRepo
    ).spendingByCategory(now.minusDays(1), now.plusDays(1))
    assertEquals(
      result,
      Seq.empty,
      clue = "expected no categories when there are no expenses"
    )

  test("spendingTrend with short range uses daily granularity"):
    val start = LocalDate.of(2025, 1, 1)
    val end = LocalDate.of(2025, 1, 5)
    val transactions = Seq(
      expense(BigDecimal("10"), start),
      expense(BigDecimal("20"), start.plusDays(2), id = 2)
    )
    val categoryRepo =
      new InMemoryCategoryRepository(Seq(Category(CategoryId(1L), "Food")))
    val result = new AnalyticsService(
      TestRepository(transactions*),
      categoryRepo
    ).spendingTrend(start, end)
    assertEquals(
      result.granularity,
      Granularity.Daily,
      clue = "expected daily granularity for short ranges"
    )
    assert(result.points.nonEmpty, clue = "expected at least one daily point")

  test("spendingTrend with medium range uses weekly granularity"):
    val start = LocalDate.of(2025, 1, 1)
    val end = start.plusDays(30)
    val transactions = Seq(expense(BigDecimal("50"), start))
    val categoryRepo =
      new InMemoryCategoryRepository(Seq(Category(CategoryId(1L), "Food")))
    val result = new AnalyticsService(
      TestRepository(transactions*),
      categoryRepo
    ).spendingTrend(start, end)
    assertEquals(
      result.granularity,
      Granularity.Weekly,
      clue = "expected weekly granularity for medium ranges"
    )

  test("spendingTrend with long range uses monthly granularity"):
    val start = LocalDate.of(2025, 1, 1)
    val end = start.plusMonths(8)
    val transactions = Seq(expense(BigDecimal("50"), start))
    val categoryRepo =
      new InMemoryCategoryRepository(Seq(Category(CategoryId(1L), "Food")))
    val result = new AnalyticsService(
      TestRepository(transactions*),
      categoryRepo
    ).spendingTrend(start, end)
    assertEquals(
      result.granularity,
      Granularity.Monthly,
      clue = "expected monthly granularity for long ranges"
    )
    assert(
      result.points.size >= 8,
      clue = "expected monthly series to cover the range"
    )

  test("spendingTrend fills missing periods with zero"):
    val start = LocalDate.of(2025, 1, 1)
    val end = LocalDate.of(2025, 1, 3)
    val transactions = Seq(expense(BigDecimal("50"), start.plusDays(2)))
    val categoryRepo =
      new InMemoryCategoryRepository(Seq(Category(CategoryId(1L), "Food")))
    val result = new AnalyticsService(
      TestRepository(transactions*),
      categoryRepo
    ).spendingTrend(start, end)
    val zeroPoints = result.points.filter(_._2 == 0.0)
    assert(
      zeroPoints.nonEmpty,
      clue = "expected missing days to be filled with zero values"
    )

  test("TrendResult periodLabel returns correct strings"):
    assert(
      TrendResult(Seq.empty, Granularity.Daily).periodLabel == "Daily",
      clue = "expected daily label"
    )
    assert(
      TrendResult(Seq.empty, Granularity.Weekly).periodLabel == "Weekly",
      clue = "expected weekly label"
    )
    assert(
      TrendResult(Seq.empty, Granularity.Monthly).periodLabel == "Monthly",
      clue = "expected monthly label"
    )
