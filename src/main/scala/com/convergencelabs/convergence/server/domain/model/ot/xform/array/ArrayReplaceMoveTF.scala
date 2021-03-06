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

package com.convergencelabs.convergence.server.domain.model.ot

import MoveDirection.Backward
import MoveDirection.Forward
import MoveDirection.Identity
import RangeIndexRelationship.After
import RangeIndexRelationship.Before
import RangeIndexRelationship.End
import RangeIndexRelationship.Start
import RangeIndexRelationship.Within

private[ot] object ArrayReplaceMoveTF extends OperationTransformationFunction[ArrayReplaceOperation, ArrayMoveOperation] {
  def transform(s: ArrayReplaceOperation, c: ArrayMoveOperation): (ArrayReplaceOperation, ArrayMoveOperation) = {
    ArrayMoveHelper.getMoveDirection(c) match {
      case Forward => transformAgainstForwardMove(s, c)
      case Backward => transformAgainstBackwardMove(s, c)
      case Identity => transformAgainstIdentityMove(s, c)
    }
  }

  private[this] def transformAgainstForwardMove(s: ArrayReplaceOperation, c: ArrayMoveOperation): (ArrayReplaceOperation, ArrayMoveOperation) = {
    ArrayMoveHelper.getRangeIndexRelationship(c, s.index) match {
      case Before | After =>
        // A-PM-1 and A-PM-5
        (s, c)
      case Start =>
        // A-PM-2
        (s.copy(index = c.toIndex), c)
      case Within | End =>
        // A-PM-3 and A-PM-4
        (s.copy(index = s.index - 1), c)
    }
  }

  private[this] def transformAgainstBackwardMove(s: ArrayReplaceOperation, c: ArrayMoveOperation): (ArrayReplaceOperation, ArrayMoveOperation) = {
    ArrayMoveHelper.getRangeIndexRelationship(c, s.index) match {
      case Before | After =>
        // A-PM-6 and A-PM-10
        (s, c)
      case End =>
        // A-PM-7
        (s.copy(index = c.toIndex), c)
      case Within | Start =>
        // A-PM-8 and A-PM-9
        (s.copy(index = s.index + 1), c)
    }
  }

  private[this] def transformAgainstIdentityMove(s: ArrayReplaceOperation, c: ArrayMoveOperation): (ArrayReplaceOperation, ArrayMoveOperation) = {
    // A-PM-11, A-PM-12, A-PM-13
    (s, c)
  }
}
