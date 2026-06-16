package com.financemanager.traits

import com.financemanager.domain.error.IOError

trait Loader[T] {
  def load(): Either[IOError, T]
}

trait Writer[T] {
  def write(data: T): Either[IOError, Unit]
}
