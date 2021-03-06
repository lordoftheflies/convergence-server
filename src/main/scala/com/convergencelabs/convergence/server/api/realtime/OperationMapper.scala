/*
 * Copyright (c) 2019 - Convergence Labs, Inc.
 *
 * This file is part of the Convergence Server, which is released under
 * the terms of the GNU General Public License version 3 (GPLv3). A copy
 * of the GPLv3 should have been provided along with this file, typically
 * located in the "LICENSE" file, which is part of this source code package.
 * Alternatively, see <https://www.gnu.org/licenses/gpl-3.0.html> for the
 * full text of the GPLv3 license, if it was not provided.
 */

package com.convergencelabs.convergence.server.api.realtime

import com.convergencelabs.convergence.proto.model._
import com.convergencelabs.convergence.server.api.realtime.ImplicitMessageConversions._
import com.convergencelabs.convergence.server.domain.model.ot._

private[realtime] object OperationMapper {

  def mapIncoming(op: OperationData): Operation = {
    op.operation match {
      case OperationData.Operation.CompoundOperation(operation) =>
        mapIncomingCompound(operation)
      case OperationData.Operation.DiscreteOperation(operation) =>
        mapIncomingDiscrete(operation)
      case OperationData.Operation.Empty =>
        // FIXME handle empty
        ???
    }
  }

  def mapIncomingCompound(op: CompoundOperationData): CompoundOperation = {
    CompoundOperation(op.operations.map(opData => mapIncomingDiscrete(opData)).toList)
  }

  // scalastyle:off cyclomatic.complexity
  // FIXME handle missing values.
  def mapIncomingDiscrete(op: DiscreteOperationData): DiscreteOperation = {
    op.operation match {
      case DiscreteOperationData.Operation.StringInsertOperation(StringInsertOperationData(id, noOp, index, value)) =>
        StringInsertOperation(id, noOp, index, value)
      case DiscreteOperationData.Operation.StringRemoveOperation(StringRemoveOperationData(id, noOp, index, value)) =>
        StringRemoveOperation(id, noOp, index, value)
      case DiscreteOperationData.Operation.StringSetOperation(StringSetOperationData(id, noOp, value)) =>
        StringSetOperation(id, noOp, value)

      case DiscreteOperationData.Operation.ArrayInsertOperation(ArrayInsertOperationData(id, noOp, idx, newVal)) =>
        ArrayInsertOperation(id, noOp, idx, newVal.get)
      case DiscreteOperationData.Operation.ArrayRemoveOperation(ArrayRemoveOperationData(id, noOp, idx)) =>
        ArrayRemoveOperation(id, noOp, idx)
      case DiscreteOperationData.Operation.ArrayMoveOperation(ArrayMoveOperationData(id, noOp, fromIdx, toIdx)) =>
        ArrayMoveOperation(id, noOp, fromIdx, toIdx)
      case DiscreteOperationData.Operation.ArrayReplaceOperation(ArrayReplaceOperationData(id, noOp, idx, newVal)) =>
        ArrayReplaceOperation(id, noOp, idx, newVal.get)
      case DiscreteOperationData.Operation.ArraySetOperation(ArraySetOperationData(id, noOp, array)) =>
        ArraySetOperation(id, noOp, array.map(messageToDataValue).toList)

      case DiscreteOperationData.Operation.ObjectSetPropertyOperation(ObjectSetPropertyOperationData(id, noOp, prop, newVal)) =>
        ObjectSetPropertyOperation(id, noOp, prop, newVal.get)
      case DiscreteOperationData.Operation.ObjectAddPropertyOperation(ObjectAddPropertyOperationData(id, noOp, prop, newVal)) =>
        ObjectAddPropertyOperation(id, noOp, prop, newVal.get)
      case DiscreteOperationData.Operation.ObjectRemovePropertyOperation(ObjectRemovePropertyOperationData(id, noOp, prop)) =>
        ObjectRemovePropertyOperation(id, noOp, prop)
      case DiscreteOperationData.Operation.ObjectSetOperation(ObjectSetOperationData(id, noOp, objectData)) =>
        val mappedData = objectData.map { case (k, v) => (k, messageToDataValue(v)) }
        ObjectSetOperation(id, noOp, mappedData)

      case DiscreteOperationData.Operation.NumberDeltaOperation(NumberDeltaOperationData(id, noOp, delta)) =>
        NumberAddOperation(id, noOp, delta)
      case DiscreteOperationData.Operation.NumberSetOperation(NumberSetOperationData(id, noOp, number)) =>
        NumberSetOperation(id, noOp, number)

      case DiscreteOperationData.Operation.BooleanSetOperation(BooleanSetOperationData(id, noOp, value)) =>
        BooleanSetOperation(id, noOp, value)

      case DiscreteOperationData.Operation.DateSetOperation(DateSetOperationData(id, noOp, value)) =>
        DateSetOperation(id, noOp, value.get)

      case DiscreteOperationData.Operation.Empty =>
        // FIXME handle error
        ???
    }
  }
  // scalastyle:on cyclomatic.complexity

  def mapOutgoing(op: Operation): OperationData = {
    op match {
      case operation: CompoundOperation =>
        OperationData().withCompoundOperation(mapOutgoingCompound(operation))
      case operation: DiscreteOperation =>
        OperationData().withDiscreteOperation(mapOutgoingDiscrete(operation))
    }
  }

  def mapOutgoingCompound(op: CompoundOperation): CompoundOperationData = {
    CompoundOperationData(op.operations.map(opData => mapOutgoingDiscrete(opData)))
  }

  // scalastyle:off cyclomatic.complexity
  def mapOutgoingDiscrete(op: DiscreteOperation): DiscreteOperationData = {
    op match {
      case StringInsertOperation(id, noOp, index, value) =>
        DiscreteOperationData().withStringInsertOperation(StringInsertOperationData(id, noOp, index, value))
      case StringRemoveOperation(id, noOp, index, value) =>
        DiscreteOperationData().withStringRemoveOperation(StringRemoveOperationData(id, noOp, index, value))
      case StringSetOperation(id, noOp, value) =>
        DiscreteOperationData().withStringSetOperation(StringSetOperationData(id, noOp, value))

      case ArrayInsertOperation(id, noOp, idx, newVal) =>
        DiscreteOperationData().withArrayInsertOperation(ArrayInsertOperationData(id, noOp, idx, Some(newVal)))
      case ArrayRemoveOperation(id, noOp, idx) =>
        DiscreteOperationData().withArrayRemoveOperation(ArrayRemoveOperationData(id, noOp, idx))
      case ArrayMoveOperation(id, noOp, fromIdx, toIdx) =>
        DiscreteOperationData().withArrayMoveOperation(ArrayMoveOperationData(id, noOp, fromIdx, toIdx))
      case ArrayReplaceOperation(id, noOp, idx, newVal) =>
        DiscreteOperationData().withArrayReplaceOperation(ArrayReplaceOperationData(id, noOp, idx, Some(newVal)))
      case ArraySetOperation(id, noOp, array) =>
        DiscreteOperationData().withArraySetOperation(ArraySetOperationData(id, noOp, array.map(dataValueToMessage)))

      case ObjectSetPropertyOperation(id, noOp, prop, newVal) =>
        DiscreteOperationData().withObjectSetPropertyOperation(ObjectSetPropertyOperationData(id, noOp, prop, Some(newVal)))
      case ObjectAddPropertyOperation(id, noOp, prop, newVal) =>
        DiscreteOperationData().withObjectAddPropertyOperation(ObjectAddPropertyOperationData(id, noOp, prop, Some(newVal)))
      case ObjectRemovePropertyOperation(id, noOp, prop) =>
        DiscreteOperationData().withObjectRemovePropertyOperation(ObjectRemovePropertyOperationData(id, noOp, prop))
      case ObjectSetOperation(id, noOp, objectData) =>
        val mappedData = objectData.map { case (k, v) => (k, dataValueToMessage(v)) }
        DiscreteOperationData().withObjectSetOperation(ObjectSetOperationData(id, noOp, mappedData))

      case NumberAddOperation(id, noOp, delta) =>
        DiscreteOperationData().withNumberDeltaOperation(NumberDeltaOperationData(id, noOp, delta))
      case NumberSetOperation(id, noOp, number) =>
        DiscreteOperationData().withNumberSetOperation(NumberSetOperationData(id, noOp, number))

      case BooleanSetOperation(id, noOp, value) =>
        DiscreteOperationData().withBooleanSetOperation(BooleanSetOperationData(id, noOp, value))

      case DateSetOperation(id, noOp, value) =>
        DiscreteOperationData().withDateSetOperation(DateSetOperationData(id, noOp, Some(value)))
    }
  }
  // scalastyle:on cyclomatic.complexity
}
