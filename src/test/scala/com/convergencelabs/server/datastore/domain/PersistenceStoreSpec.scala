package com.convergencelabs.server.datastore.domain

import java.time.Instant
import java.util.concurrent.atomic.AtomicInteger

import com.convergencelabs.server.db.{ConnectedSingleDatabaseProvider, DatabaseProvider}
import com.convergencelabs.server.db.schema.{DeltaCategory, TestingSchemaManager}
import com.orientechnologies.common.log.OLogManager
import com.orientechnologies.orient.core.db.{ODatabaseType, OrientDB, OrientDBConfig}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpec}

object PersistenceStoreSpec {
  val OrientDBAdmin = "admin"
}

abstract class PersistenceStoreSpec[S](category: DeltaCategory.Value)
  extends WordSpec
  with Matchers
  with BeforeAndAfterAll {
  
  import PersistenceStoreSpec._
  
  OLogManager.instance().setConsoleLevel("WARNING")

  private[this] val dbCounter = new AtomicInteger(1)
  private[this] val orientDB: OrientDB = new OrientDB(s"memory:target/orientdb/PersistenceStoreSpec/${getClass.getSimpleName}", OrientDBConfig.defaultConfig)

  override protected def afterAll(): Unit = {
    orientDB.close()
  }
  
  def withPersistenceStore(testCode: S => Any): Unit = {
    // make sure no accidental collisions
    val dbName = s"${getClass.getSimpleName}/${nextDbId()}"

    orientDB.create(dbName, ODatabaseType.MEMORY)
    val db = orientDB.open(dbName, OrientDBAdmin, OrientDBAdmin)
    val dbProvider = new ConnectedSingleDatabaseProvider(db)

    try {
      dbProvider.connect().get
      val mgr = new TestingSchemaManager(db, category, true)
      mgr.install().get
      val store = createStore(dbProvider)
      testCode(store)
    } finally {
      dbProvider.shutdown()
      orientDB.drop(dbName)
    }
  }

  private[this] def nextDbId(): Int = {
    dbCounter.getAndIncrement()
  }

  protected def truncatedInstantNow(): Instant = {
    java.util.Date.from(Instant.now()).toInstant
  }
  
  protected def createStore(dbProvider: DatabaseProvider): S
}