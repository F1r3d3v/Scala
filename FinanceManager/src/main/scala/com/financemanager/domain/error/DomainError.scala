package com.financemanager.domain.error

import com.financemanager.domain.model.TransactionId

/** Domain-level validation and repository errors.
  *
  * @param message
  *   user-facing error description
  */
enum DomainError(val message: String):
  case InvalidAmount(msg: String) extends DomainError(msg)
  case InvalidCategory(msg: String) extends DomainError(msg)
  case InvalidDescription(msg: String) extends DomainError(msg)
  case NotFound(id: TransactionId)
      extends DomainError(s"Transaction with id ${id.value} not found")
  case SystemError(msg: String) extends DomainError(msg)
