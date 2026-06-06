package com.financemanager.presentation.view

import com.financemanager.domain.model.{Category, TransactionId, TransactionInput, TransactionType, CategoryId}
import com.financemanager.presentation.*
import com.financemanager.presentation.DisplayModels.*
import javafx.beans.property.ReadOnlyStringWrapper
import javafx.collections.FXCollections
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.geometry.Side
import javafx.scene.Node
import javafx.scene.control.*
import javafx.scene.layout.{GridPane, HBox, Priority, VBox, Region}
import javafx.stage.FileChooser

import java.nio.file.Path
import java.time.LocalDate
import scala.compiletime.uninitialized
import scala.util.Try

/**
 * View for the transactions tab, including the list and add/edit form.
 */
final class TransactionsView extends TransactionsViewContract:
  /** Presenter assigned by the application during wiring. */
  var presenter: TransactionsPresenterContract = uninitialized

  private val transactionsList = FXCollections.observableArrayList[TransactionDisplay]()
  private var selectedDisplayId: Option[TransactionId] = None

  private val table = new TableView[TransactionDisplay](transactionsList)
  table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS)

  private val dateCol = new TableColumn[TransactionDisplay, String]("Date")
  dateCol.setCellValueFactory(cell => ReadOnlyStringWrapper(cell.getValue.date))

  private val amountCol = new TableColumn[TransactionDisplay, String]("Amount")
  amountCol.setCellValueFactory(cell => ReadOnlyStringWrapper(cell.getValue.amount))

  private val categoryCol = new TableColumn[TransactionDisplay, String]("Category")
  categoryCol.setCellValueFactory(cell => ReadOnlyStringWrapper(cell.getValue.category))

  private val descriptionCol = new TableColumn[TransactionDisplay, String]("Description")
  descriptionCol.setCellValueFactory(cell => ReadOnlyStringWrapper(cell.getValue.description))

  table.getColumns.addAll(dateCol, amountCol, categoryCol, descriptionCol)

  private val datePicker = new DatePicker(LocalDate.now())
  private val amountField = new TextField()
  amountField.setPromptText("e.g. 49.99")

  private val categoryBox = new ComboBox[String]()
  private var categoriesCache: Seq[Category] = Seq.empty
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

  private val importItem = new MenuItem("Import")
  private val exportItem = new MenuItem("Export")
  private val importExportMenu = new ContextMenu(importItem, exportItem)
  private val importExportButton = new Button("☰")
  importExportButton.setOnAction(_ =>
    if importExportMenu.isShowing then importExportMenu.hide()
    else importExportMenu.show(importExportButton, Side.BOTTOM, 0, 0)
  )

  importItem.setOnAction(_ => chooseCsvFile(isImport = true).foreach(presenter.onImport))
  exportItem.setOnAction(_ => chooseCsvFile(isImport = false).foreach(presenter.onExport))

  val root: Node =
    val container = new VBox(14)
    container.setPadding(new Insets(20))

    val title = new Label("Transactions")
    title.setStyle("-fx-font-size: 24; -fx-font-weight: bold;")

    val headerSpacer = new Region()
    HBox.setHgrow(headerSpacer, Priority.ALWAYS)
    val header = new HBox(10, title, headerSpacer, importExportButton)
    header.setAlignment(Pos.CENTER_LEFT)

    VBox.setVgrow(table, Priority.ALWAYS)
    container.getChildren.addAll(header, table, formSection)
    container

  table.getSelectionModel.selectedItemProperty.addListener((_, _, selected) =>
    if selected != null then presenter.onTransactionSelected(selected.id)
  )

  saveButton.setOnAction(_ => handleSubmit())
  deleteButton.setOnAction(_ => selectedDisplayId.foreach(presenter.onDelete))
  clearButton.setOnAction(_ => presenter.onClearSelection())

  override def displayTransactions(transactions: Seq[TransactionDisplay]): Unit =
    transactionsList.setAll(transactions*)

  override def displayCategories(categories: Seq[Category]): Unit =
    categoriesCache = categories
    categoryBox.setItems(FXCollections.observableArrayList(categories.map(_.name)*))

  override def displayError(message: String): Unit =
    val alert = new Alert(Alert.AlertType.ERROR)
    alert.setTitle("Validation error")
    alert.setHeaderText("Cannot save transaction")
    alert.setContentText(message)
    alert.showAndWait()

  override def resetForm(): Unit =
    selectedDisplayId = None
    datePicker.setValue(LocalDate.now())
    amountField.clear()
    categoryBox.getSelectionModel.clearSelection()
    descriptionField.clear()
    table.getSelectionModel.clearSelection()
    saveButton.setText("Add")
    saveButton.setStyle("")
    deleteButton.setDisable(true)
    isIncomeCheckbox.setSelected(false)

  override def populateForm(transaction: TransactionDisplay): Unit =
    selectedDisplayId = Some(transaction.id)
    datePicker.setValue(LocalDate.parse(transaction.date))
    amountField.setText(transaction.rawAmount.toString)
    categoryBox.getSelectionModel.select(transaction.category)
    descriptionField.setText(transaction.description)
    saveButton.setText("Update")
    saveButton.setStyle("-fx-base: #3498db;")
    deleteButton.setDisable(false)
    isIncomeCheckbox.setSelected(transaction.transactionType == TransactionType.Income)

  private def formSection: VBox =
    val wrapper = new VBox(10)
    val subtitle = new Label("Add or Edit Transaction")
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

  private def handleSubmit(): Unit =
    buildInput() match
      case Left(error) => displayError(error)
      case Right(input) => presenter.onSubmit(input)

  private def chooseCsvFile(isImport: Boolean): Option[Path] =
    val chooser = new FileChooser()
    chooser.setTitle(if isImport then "Import Transactions CSV" else "Export Transactions CSV")
    chooser.getExtensionFilters.add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"))
    if !isImport then chooser.setInitialFileName("transactions.csv")

    val window = Option(root.getScene).map(_.getWindow).orNull
    val file = if isImport then chooser.showOpenDialog(window) else chooser.showSaveDialog(window)
    Option(file).map(_.toPath)

  private def buildInput(): Either[String, TransactionInput] =
    val date = Option(datePicker.getValue)
    val amount = Try(BigDecimal(amountField.getText.trim)).toOption
    val selectedName = Option(categoryBox.getSelectionModel.getSelectedItem).map(_.trim)
    val categoryId = selectedName.flatMap(name => categoriesCache.find(_.name == name).map(_.id)).getOrElse(CategoryId(0L))
    val description = Option(descriptionField.getText).map(_.trim).getOrElse("")
    val isIncome = isIncomeCheckbox.isSelected

    if date.isEmpty then Left("Date is required")
    else if amount.isEmpty then Left("Amount must be a valid number")
    else Right(TransactionInput(date.get, amount.get, categoryId, description, if isIncome then TransactionType.Income else TransactionType.Expense))
