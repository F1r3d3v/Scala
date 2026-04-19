package com.financemanager.presentation.ui.transactions

import com.financemanager.model.{Transaction, TransactionInput}
import com.financemanager.presentation.contracts.{ExpenseCommands, ExpenseDataSource}
import javafx.beans.property.ReadOnlyStringWrapper
import javafx.collections.FXCollections
import javafx.geometry.Insets
import javafx.scene.Node
import javafx.scene.control.*
import javafx.scene.layout.{GridPane, HBox, Priority, VBox}

import java.time.LocalDate
import scala.util.Try

/**
 * Provides CRUD interactions for expenses, including table view and edit form.
 */
final class TransactionsView(dataSource: ExpenseDataSource, commands: ExpenseCommands):
  private var selectedExpenseId: Option[Long] = None

  private val table = new TableView[Transaction](dataSource.expenses)
  table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS)

  private val dateCol = new TableColumn[Transaction, String]("Date")
  dateCol.setCellValueFactory(cell => ReadOnlyStringWrapper(cell.getValue.date.toString))

  private val amountCol = new TableColumn[Transaction, String]("Amount")
  amountCol.setCellValueFactory { cell =>
    val exp = cell.getValue
    val prefix = if exp.isIncome then "+$" else "-$"
    ReadOnlyStringWrapper(prefix + f"${exp.amount.toDouble}%.2f")
  }

  private val categoryCol = new TableColumn[Transaction, String]("Category")
  categoryCol.setCellValueFactory(cell => ReadOnlyStringWrapper(cell.getValue.category))

  private val descriptionCol = new TableColumn[Transaction, String]("Description")
  descriptionCol.setCellValueFactory(cell => ReadOnlyStringWrapper(cell.getValue.description))

  table.getColumns.addAll(dateCol, amountCol, categoryCol, descriptionCol)

  private val datePicker = new DatePicker(LocalDate.now())
  private val amountField = new TextField()
  amountField.setPromptText("e.g. 49.99")

  private val categoryBox = new ComboBox[String](FXCollections.observableArrayList(commands.categories*))
  categoryBox.setPromptText("Select category")

  private val descriptionField = new TextArea()
  descriptionField.setPromptText("Description")
  descriptionField.setPrefRowCount(3)
  descriptionField.setWrapText(true)

  private val saveButton = new Button("Add")

  private val deleteButton = new Button("Delete")
  deleteButton.setDisable(true)

  private val clearButton = new Button("Clear")

  private val isIncomeCheckbox = new CheckBox("Mark as Income")

  /** Root node rendered in the Transactions tab. */
  val root: Node =
    val container = new VBox(14)
    container.setPadding(new Insets(20))

    val title = new Label("Expenses")
    title.setStyle("-fx-font-size: 24; -fx-font-weight: bold;")

    VBox.setVgrow(table, Priority.ALWAYS)
    container.getChildren.addAll(title, table, formSection)
    container

  table.getSelectionModel.selectedItemProperty.addListener((_, _, selected) =>
    if selected != null then loadExpenseIntoForm(selected)
  )

  saveButton.setOnAction(_ => handleSave())
  deleteButton.setOnAction(_ => deleteExpense())
  clearButton.setOnAction(_ => clearForm())

  /** Builds the form section used to add or edit an expense. */
  private def formSection: VBox =
    val wrapper = new VBox(10)

    val subtitle = new Label("Add or Edit Expense")
    subtitle.setStyle("-fx-font-size: 18; -fx-font-weight: bold;")

    val grid = new GridPane()
    grid.setHgap(12)
    grid.setVgap(10)
    grid.add(new Label("Date"), 0, 0)
    grid.add(datePicker, 1, 0)
    grid.add(new Label("Amount"), 0, 1)
    grid.add(amountField, 1, 1)
    grid.add(new Label("Type"), 0, 2)
    grid.add(isIncomeCheckbox, 1, 2)
    grid.add(new Label("Category"), 2, 0)
    grid.add(categoryBox, 3, 0)
    grid.add(new Label("Description"), 2, 1)
    grid.add(descriptionField, 3, 1)

    val buttons = new HBox(10, saveButton, deleteButton, clearButton)

    wrapper.getChildren.addAll(subtitle, grid, buttons)
    wrapper

  /** Loads selected table row values into the edit form. */
  private def loadExpenseIntoForm(expense: Transaction): Unit =
    selectedExpenseId = Some(expense.id)
    datePicker.setValue(expense.date)
    amountField.setText(expense.amount.toString)
    categoryBox.getSelectionModel.select(expense.category)
    descriptionField.setText(expense.description)
    saveButton.setText("Update")
    saveButton.setStyle("-fx-base: #3498db;")
    deleteButton.setDisable(false)
    isIncomeCheckbox.setSelected(expense.isIncome)

  /** Validates and submits a new expense using command handlers. */
  private def submitNewExpense(): Unit =
    buildInput() match
      case Left(error) => showError(error)
      case Right(input) =>
        commands.addExpense(input) match
          case Left(validationError) => showError(validationError)
          case Right(_) => clearForm()

  /** Validates and persists changes for the currently selected expense. */
  private def updateExpense(): Unit =
    selectedExpenseId match
      case None => showError("Select an expense first")
      case Some(id) =>
        buildInput() match
          case Left(error) => showError(error)
          case Right(input) =>
            commands.updateExpense(id, input) match
              case Left(validationError) => showError(validationError)
              case Right(_) => clearForm()

  /** Deletes the currently selected expense and resets the form. */
  private def deleteExpense(): Unit =
    selectedExpenseId.foreach(commands.deleteExpense)
    clearForm()

  /** Clears form fields and returns UI controls to initial state. */
  private def clearForm(): Unit =
    selectedExpenseId = None
    datePicker.setValue(LocalDate.now())
    amountField.clear()
    categoryBox.getSelectionModel.clearSelection()
    descriptionField.clear()
    table.getSelectionModel.clearSelection()
    saveButton.setText("Add")
    saveButton.setStyle("")
    deleteButton.setDisable(true)
    isIncomeCheckbox.setSelected(false)

  /** Builds domain input from form controls with basic parsing checks. */
  private def buildInput(): Either[String, TransactionInput] =
    val date = Option(datePicker.getValue)
    val amount = Try(BigDecimal(amountField.getText.trim)).toOption
    val category = Option(categoryBox.getSelectionModel.getSelectedItem).map(_.trim).getOrElse("")
    val description = Option(descriptionField.getText).map(_.trim).getOrElse("")
    val isIncome = isIncomeCheckbox.isSelected

    if date.isEmpty then Left("Date is required")
    else if amount.isEmpty then Left("Amount must be a valid number")
    else Right(TransactionInput(date.get, amount.get, category, description, isIncome))

  /** Shows a validation error dialog to the user. */
  private def showError(message: String): Unit =
    val alert = new Alert(Alert.AlertType.ERROR)
    alert.setTitle("Validation error")
    alert.setHeaderText("Cannot save expense")
    alert.setContentText(message)
    alert.showAndWait()

  private def handleSave() : Unit =
    if selectedExpenseId.isDefined then
      updateExpense()
    else
      submitNewExpense()