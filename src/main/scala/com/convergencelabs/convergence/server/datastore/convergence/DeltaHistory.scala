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

package com.convergencelabs.convergence.server.datastore.convergence

import java.time.Instant
import com.convergencelabs.convergence.server.domain.DomainId

case class ConvergenceDelta(deltaNo: Int, value: String)
case class ConvergenceDeltaHistory(delta: ConvergenceDelta, status: String, message: Option[String], date: Instant)

case class DomainDelta(deltaNo: Int, value: String)
case class DomainDeltaHistory(domain: DomainId, delta: DomainDelta, status: String, message: Option[String], date: Instant)