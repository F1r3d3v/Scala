package com.financemanager.presentation.view

import com.financemanager.domain.model.{Category, CategoryId, ImportMode, TransactionId, TransactionInput, TransactionType}
import com.financemanager.domain.service.CategoryDeletionPreview
import com.financemanager.presentation.*
import com.financemanager.presentation.DisplayModels.*
import javafx.beans.property.ReadOnlyStringWrapper
import javafx.collections.FXCollections
import javafx.geometry.{Insets, Pos, Side}
import javafx.scene.Node
import javafx.scene.control.*
import javafx.scene.layout.{GridPane, HBox, Priority, Region, VBox}
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

  private val manageCategoriesButton = new Button("Edit")
  manageCategoriesButton.setFocusTraversable(false)
  manageCategoriesButton.setTooltip(new Tooltip("Manage categories"))

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
  private val importExportButton = new Button("Menu")
  importExportButton.setOnAction(_ =>
    if importExportMenu.isShowing then importExportMenu.hide()
    else importExportMenu.show(importExportButton, Side.BOTTOM, 0, 0)
  )

  importItem.setOnAction(_ =>
    for
      mode <- chooseImportMode()
      path <- chooseCsvFile(isImport = true)
    do presenter.onImport(path, mode)
  )
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
  manageCategoriesButton.setOnAction(_ => showCategoryManager())

  override def displayTransactions(transactions: Seq[TransactionDisplay]): Unit =
    transactionsList.setAll(transactions*)

  override def displayCategories(categories: Seq[Category]): Unit =
    val selectedName = Option(categoryBox.getSelectionModel.getSelectedItem)
    categoriesCache = categories
    categoryBox.setItems(FXCollections.observableArrayList(categories.map(_.name)*))
    selectedName.foreach(name => if categories.exists(_.name == name) then categoryBox.getSelectionModel.select(name))

  override def displayError(message: String): Unit =
    val alert = new Alert(Alert.AlertType.ERROR)
    alert.setTitle("Validation error")
    alert.setHeaderText("Cannot save transaction")
    alert.setContentText(message)
    alert.showAndWait()

  override def confirmCategoryDeletion(preview: CategoryDeletionPreview): Boolean =
    val dialog = new Dialog[Boolean]()
    dialog.setTitle("Delete category")
    dialog.setHeaderText(null)
    dialog.setGraphic(null)

    val window = Option(root.getScene).map(_.getWindow).orNull
    dialog.initOwner(window)

    val deleteButtonType = new ButtonType("Delete", ButtonBar.ButtonData.OK_DONE)
    dialog.getDialogPane.getButtonTypes.addAll(deleteButtonType, ButtonType.CANCEL)

    val headline =
      if preview.assignedTransactionCount > 0 then
        s"""Category "${preview.category.name}" is assigned to ${preview.assignedTransactionCount} transaction(s)."""
      else s"""Delete category "${preview.category.name}"?"""

    val details =
      if preview.assignedTransactionCount > 0 then
        s"""Those transactions will be kept and reassigned to "${preview.replacementCategoryName}"."""
      else "This action removes the category from the list."

    val content = new VBox(
      10,
      new Label(headline),
      new Label(details)
    )
    content.setPadding(new Insets(6, 12, 0, 12))
    dialog.getDialogPane.setContent(content)
    dialog.setResultConverter(buttonType => buttonType == deleteButtonType)

    Option(dialog.showAndWait().orElse(false)).contains(true)

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

    val categoryControls = new HBox(8, categoryBox, manageCategoriesButton)
    categoryControls.setAlignment(Pos.CENTER_LEFT)
    HBox.setHgrow(categoryBox, Priority.ALWAYS)
    grid.add(categoryControls, 3, 0)

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

  private def chooseImportMode(): Option[ImportMode] =
    val dialog = new Dialog[ImportMode]()
    dialog.setTitle("Import transactions")
    dialog.setHeaderText(null)
    dialog.setGraphic(null)

    val window = Option(root.getScene).map(_.getWindow).orNull
    dialog.initOwner(window)

    val continueButtonType = new ButtonType("Continue", ButtonBar.ButtonData.OK_DONE)
    dialog.getDialogPane.getButtonTypes.addAll(continueButtonType, ButtonType.CANCEL)

    val appendRadio = new RadioButton("Add imported transactions to the current list")
    val overwriteRadio = new RadioButton("Replace current transactions with imported data")
    val toggleGroup = new ToggleGroup()
    appendRadio.setToggleGroup(toggleGroup)
    overwriteRadio.setToggleGroup(toggleGroup)
    appendRadio.setSelected(true)

    val caption = new Label("Choose how the CSV should be applied.")
    caption.setWrapText(true)

    val content = new VBox(12, caption, appendRadio, overwriteRadio)
    content.setPadding(new Insets(6, 12, 0, 12))
    dialog.getDialogPane.setContent(content)

    dialog.setResultConverter(buttonType =>
      if buttonType == continueButtonType then
        if overwriteRadio.isSelected then ImportMode.Overwrite else ImportMode.Append
      else null
    )

    Option(dialog.showAndWait().orElse(null))

  private def showCategoryManager(): Unit =
    val dialog = new Dialog[Unit]()
    dialog.setTitle("Categories")
    dialog.setHeaderText(null)
    dialog.setGraphic(null)

    val window = Option(root.getScene).map(_.getWindow).orNull
    dialog.initOwner(window)
    dialog.getDialogPane.getButtonTypes.add(ButtonType.CLOSE)

    val categoryList = new ListView[String](FXCollections.observableArrayList(categoriesCache.map(_.name)*))
    categoryList.setPrefHeight(180)

    val nameField = new TextField()
    nameField.setPromptText("New category")

    val addButton = new Button("Add")
    val removeButton = new Button("Remove")
    removeButton.disableProperty.bind(categoryList.getSelectionModel.selectedItemProperty.isNull)

    addButton.setOnAction(_ =>
      val trimmed = Option(nameField.getText).map(_.trim).getOrElse("")
      if trimmed.nonEmpty then
        presenter.onAddCategory(trimmed)
        nameField.clear()
        categoryList.setItems(FXCollections.observableArrayList(categoriesCache.map(_.name)*))
    )

    removeButton.setOnAction(_ =>
      Option(categoryList.getSelectionModel.getSelectedItem)
        .flatMap(name => categoriesCache.find(_.name == name))
        .foreach { category =>
          presenter.onDeleteCategory(category.id)
          categoryList.setItems(FXCollections.observableArrayList(categoriesCache.map(_.name)*))
        }
    )

    val addRow = new HBox(8, nameField, addButton)
    addRow.setAlignment(Pos.CENTER_LEFT)
    HBox.setHgrow(nameField, Priority.ALWAYS)

    val content = new VBox(12, new Label("Quick category management"), categoryList, addRow, removeButton)
    content.setPadding(new Insets(6, 12, 0, 12))
    dialog.getDialogPane.setContent(content)

    dialog.showAndWait()

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
