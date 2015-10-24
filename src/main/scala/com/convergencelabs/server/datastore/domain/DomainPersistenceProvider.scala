package com.convergencelabs.server.datastore.domain

import org.json4s.JsonAST.JValue
import com.orientechnologies.orient.core.db.OPartitionedDatabasePool
import scala.util.Try
import scala.util.Success
import scala.util.Failure

class DomainPersistenceProvider(private[domain] val dbPool: OPartitionedDatabasePool) {

  val userStore = new DomainUserStore(dbPool)

  val modelStore = new ModelStore(dbPool)

  val modelHistoryStore = new ModelHistoryStore(dbPool)

  val modelSnapshotStore = new ModelSnapshotStore(dbPool)
  
  def validateConnection(): Boolean = {
    Try[Unit](dbPool.acquire().close()) match {
      case Success(_) => true
      case Failure(_) => false
    }
  }
}