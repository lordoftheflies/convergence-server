package com.convergencelabs.server.datastore.domain

import com.orientechnologies.orient.core.db.OPartitionedDatabasePool
import com.convergencelabs.server.datastore.AbstractPersistenceProvider

class DomainPersistenceProvider(private[this] val dbPool: OPartitionedDatabasePool) extends AbstractPersistenceProvider(dbPool) {

  val userStore = new DomainUserStore(dbPool)

  val keyStore = new ApiKeyStore(dbPool)

  val configStore = new DomainConfigStore(dbPool)

  val modelOperationStore = new ModelOperationStore(dbPool)

  val modelSnapshotStore = new ModelSnapshotStore(dbPool)

  val modelStore = new ModelStore(dbPool, modelOperationStore, modelSnapshotStore)

  val collectionStore = new CollectionStore(dbPool, modelStore: ModelStore)

  val modelOperationProcessor = new ModelOperationProcessor(dbPool)

}