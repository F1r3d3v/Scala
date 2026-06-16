package com.financemanager.domain.repository

import scala.collection.mutable.ListBuffer

/** Mixin for repository change notifications.
  */
trait Subscribable:
  private val listeners = ListBuffer.empty[() => Unit]

  def subscribe(listener: () => Unit): Unit = listeners += listener
  protected def notifyListeners(): Unit = listeners.foreach(_())
