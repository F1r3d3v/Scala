package com.financemanager.presentation.ui.main

import javafx.scene.Node
import javafx.scene.control.{Tab, TabPane}
import javafx.scene.layout.BorderPane

/**
 * Hosts the three main application sections inside a tabbed layout.
 */
final class MainView(
    dashboardView: Node,
    transactionsView: Node,
    analyticsView: Node
):
  /** Root node used as the main scene content. */
  val root: BorderPane =
    val tabs = new TabPane()
    tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE)

    val dashboardTab = new Tab("Dashboard", dashboardView)
    val transactionsTab = new Tab("Transactions", transactionsView)
    val analyticsTab = new Tab("Analytics", analyticsView)

    tabs.getTabs.addAll(dashboardTab, transactionsTab, analyticsTab)

    val container = new BorderPane()
    container.setCenter(tabs)
    container
