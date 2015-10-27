package com.convergencelabs.server.datastore.domain

import com.orientechnologies.orient.core.db.OPartitionedDatabasePool
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx
import com.orientechnologies.orient.core.db.tool.ODatabaseImport
import com.orientechnologies.orient.core.command.OCommandOutputListener
import com.orientechnologies.common.log.OLogManager

trait PersistenceStoreSpec[S] {
  OLogManager.instance().setConsoleLevel("WARNING")

  def createStore(dbPool: OPartitionedDatabasePool): S

  var dbCounter = 0
  def withPersistenceStore(testCode: S => Any) {
    // make sure no accidental collisions
    val dbName = getClass.getSimpleName
    val uri = "memory:${dbName}${dbCounter}"
    dbCounter += 1

    val db = new ODatabaseDocumentTx(uri)
    db.activateOnCurrentThread()
    db.create()

    val file = getClass.getResource("/dbfiles/t1.gz").getFile()
    val dbImport = new ODatabaseImport(db, file, CommandListener)
    dbImport.importDatabase()
    dbImport.close()

    val dbPool = new OPartitionedDatabasePool(uri, "admin", "admin")
    val store = createStore(dbPool)

    try {
      testCode(store)
    } finally {
      dbPool.close()
      db.activateOnCurrentThread()
      db.drop() // Drop will close and drop
    }
  }

  object CommandListener extends OCommandOutputListener() {
    def onMessage(iText: String) = {
    }
  }

}