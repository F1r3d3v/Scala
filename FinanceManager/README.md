# FinanceManager - Presentation Layer

This project contains a JavaFX-based presentation layer for a Personal Finance Manager desktop app, configured in `build.sbt` with JavaFX (and ScalaFX dependency available for future migration).

## Implemented UI

- Main JavaFX application (`Stage`) in `com.financemanager.app.FinanceManagerApp`
- Tab navigation: Dashboard, Transactions, Analytics
- Dashboard mock summary cards: total spent, budget remaining, transactions this month
- Transactions view with expense `TableView` and add/update/delete form
- Analytics view with category pie chart and last 6 months bar chart
- Decoupled contracts for later business-logic injection:
  - `ExpenseDataSource`
  - `ExpenseCommands`
  - `BudgetProvider`

## Run

```powershell
sbt run
```

The app starts with SQLite persistence enabled and stores data at `~/.financemanager/finance.db` by default.
