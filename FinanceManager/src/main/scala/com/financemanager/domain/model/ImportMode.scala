package com.financemanager.domain.model

/** Controls how imported transactions should be merged into persisted data.
  */
enum ImportMode:
  case Append
  case Overwrite
