package com.financemanager.IO.csv

import com.financemanager.domain.error.IOError

trait CsvCodec[T]:
  def headers: Seq[String]
  def encode(value: T): Seq[String]
  def decode(fields: Seq[String]): Either[IOError, T]

