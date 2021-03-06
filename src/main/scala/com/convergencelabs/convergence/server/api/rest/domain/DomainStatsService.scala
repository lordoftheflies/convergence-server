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

package com.convergencelabs.convergence.server.api.rest.domain

import akka.actor.ActorRef
import akka.http.scaladsl.marshalling.ToResponseMarshallable.apply
import akka.http.scaladsl.server.Directive.addByNameNullaryApply
import akka.http.scaladsl.server.Directives.{_segmentStringToPathMatcher, complete, get, path}
import akka.http.scaladsl.server.Route
import akka.pattern.ask
import akka.util.Timeout
import com.convergencelabs.convergence.server.api.rest.{RestResponse, okResponse}
import com.convergencelabs.convergence.server.datastore.domain.DomainStatsActor.{DomainStats, GetStats}
import com.convergencelabs.convergence.server.domain.DomainId
import com.convergencelabs.convergence.server.domain.rest.RestDomainActor.DomainRestMessage
import com.convergencelabs.convergence.server.security.AuthorizationProfile

import scala.concurrent.{ExecutionContext, Future}

object DomainStatsService {

  case class DomainStatsRestData(activeSessionCount: Long, userCount: Long, modelCount: Long, dbSize: Long)

}

class DomainStatsService(private[this] val executionContext: ExecutionContext,
                         private[this] val timeout: Timeout,
                         private[this] val domainRestActor: ActorRef)
  extends DomainRestService(executionContext, timeout) {

  import DomainStatsService._

  def route(authProfile: AuthorizationProfile, domain: DomainId): Route =
    (path("stats") & get) {
      complete(getStats(domain))
    }

  private[this] def getStats(domain: DomainId): Future[RestResponse] = {
    (domainRestActor ? DomainRestMessage(domain, GetStats))
      .mapTo[DomainStats]
      .map { stats =>
        val DomainStats(activeSessionCount, userCount, modelCount, dbSize) = stats
        okResponse(DomainStatsRestData(activeSessionCount, userCount, modelCount, dbSize))

      }
  }
}
