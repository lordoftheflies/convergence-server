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

package com.convergencelabs.convergence.server.domain.model

import com.convergencelabs.convergence.server.datastore.domain.{DomainPersistenceProvider, ModelPermissions}
import com.convergencelabs.convergence.server.domain.DomainUserId

import scala.util.{Failure, Success, Try}

case class ModelPermissionResult(
                                  overrideCollection: Boolean,
                                  modelWorld: ModelPermissions,
                                  modelUsers: Map[DomainUserId, ModelPermissions])

class ModelPermissionResolver() {
  def getModelUserPermissions(modelId: String, userId: DomainUserId, persistenceProvider: DomainPersistenceProvider): Try[ModelPermissions] = {
    if (userId.isConvergence) {
      Success(ModelPermissions(read = true, write = true, remove = true, manage = true))
    } else {
      val permissionsStore = persistenceProvider.modelPermissionsStore
      permissionsStore.getUsersCurrentModelPermissions(modelId, userId) flatMap  {
        case Some(p) => Success(p)
        case None => Failure(ModelNotFoundException(modelId))
      }
    }
  }

  def getModelAndCollectionPermissions(modelId: String, collectionId: String, persistenceProvider: DomainPersistenceProvider): Try[RealTimeModelPermissions] = {
    for {
      overrideCollection <- persistenceProvider.modelPermissionsStore.modelOverridesCollectionPermissions(modelId)
      collectionWorld <- persistenceProvider.modelPermissionsStore.getCollectionWorldPermissions(collectionId)
      collectionUsers <- persistenceProvider.modelPermissionsStore.getAllCollectionUserPermissions(collectionId)
      modelWorld <- persistenceProvider.modelPermissionsStore.getModelWorldPermissions(modelId)
      modelUsers <- persistenceProvider.modelPermissionsStore.getAllModelUserPermissions(modelId)
    } yield {
      RealTimeModelPermissions(
        overrideCollection,
        collectionWorld,
        collectionUsers,
        modelWorld,
        modelUsers)
    }
  }

  def getModelPermissions(modelId: String, persistenceProvider: DomainPersistenceProvider): Try[ModelPermissionResult] = {
    for {
      overrideCollection <- persistenceProvider.modelPermissionsStore.modelOverridesCollectionPermissions(modelId)
      modelWorld <- persistenceProvider.modelPermissionsStore.getModelWorldPermissions(modelId)
      modelUsers <- persistenceProvider.modelPermissionsStore.getAllModelUserPermissions(modelId)
    } yield {
      ModelPermissionResult(
        overrideCollection,
        modelWorld,
        modelUsers)
    }
  }
}
