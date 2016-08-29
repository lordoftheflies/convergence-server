package com.convergencelabs.server.domain.model

import com.convergencelabs.server.domain.model.ot.Operation
import scala.util.Try
import com.convergencelabs.server.domain.model.ot.DiscreteOperation
import scala.util.Failure
import com.convergencelabs.server.domain.model.reference.ReferenceManager
import com.convergencelabs.server.domain.model.reference.ModelReference


abstract class RealTimeValue(
    private[model] val id: String,
    private[model] val model: RealTimeModel,
    private[model] var parent: Option[RealTimeContainerValue],
    private[model] var parentField: Option[Any],
    validReferenceTypes: List[ReferenceType.Value]) {

  model.registerValue(this)

  protected val referenceManager = new ReferenceManager(this, validReferenceTypes)
  protected var listeners: List[String => Unit] = Nil

  def path(): List[Any] = {
    parent match {
      case None => List()
      case Some(p) => p.path() :+ parentField
    }
  }

  def addListener(listener: String => Unit) {
    listeners ::= listener
  }
  
  def removeListener(listener: String => Unit) {
    listeners filter(!_.equals(listener))
  }
  
  def detach(): Unit = {
    for (listener <- listeners) listener(id)
    model.unregisterValue(this)
  }

  def data(): Any

  def processOperation(operation: DiscreteOperation): Try[Unit]

  def references(): Set[ModelReference[_]] = {
    this.referenceManager.referenceMap().getAll()
  }

  def sessionDisconnected(sessionId: String): Unit = {
    this.referenceManager.sessionDisconnected(sessionId)
  }

  def processReferenceEvent(event: ModelReferenceEvent, sessionId: String): Try[Unit] = Try {
    if (this.validReferenceTypes.isEmpty) {
      throw new IllegalArgumentException("This value does not allow references")
    }
    this.referenceManager.handleReferenceEvent(event, sessionId)
  }
}