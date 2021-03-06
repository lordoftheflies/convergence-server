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

package com.convergencelabs.convergence.server.datastore.domain

import java.time.Instant

import org.scalatest.Matchers
import org.scalatest.OptionValues.convertOptionToValuable
import org.scalatest.TryValues.convertTryToSuccessOrFailure
import org.scalatest.WordSpecLike

import com.convergencelabs.convergence.server.db.DatabaseProvider
import com.convergencelabs.convergence.server.db.schema.DeltaCategory
import com.convergencelabs.convergence.server.domain.model.Model
import com.convergencelabs.convergence.server.domain.model.ModelMetaData
import com.convergencelabs.convergence.server.domain.model.ModelSnapshot
import com.convergencelabs.convergence.server.domain.model.ModelSnapshotMetaData
import com.convergencelabs.convergence.server.domain.model.data.DoubleValue
import com.convergencelabs.convergence.server.domain.model.data.ObjectValue
import com.convergencelabs.convergence.server.domain.model.data.StringValue

import java.util.Date

// scalastyle:off magic.number
class ModelSnapshotStoreSpec
    extends PersistenceStoreSpec[DomainPersistenceProvider](DeltaCategory.Domain)
    with WordSpecLike
    with Matchers {

  override def createStore(dbProvider: DatabaseProvider): DomainPersistenceProvider =
    new DomainPersistenceProviderImpl(dbProvider)

  val modelPermissions = ModelPermissions(true, true, true, true)
  
  val CollectionId = "people"

  val person1Id = "person1"
  val person1ModelData = ObjectValue("vid", Map("value" -> DoubleValue("0:1", 1)))
  val person1ModelMetaData = ModelMetaData(person1Id, CollectionId, 10, Instant.now(), Instant.now(), true, modelPermissions, 1)
  val person1Model = Model(person1ModelMetaData, person1ModelData)

  val p1Snapshot1Version = 1L
  val p1Snapshot1Date = Instant.parse("2016-01-01T00:00:00Z")
  val p1Snapshot1MetaData = ModelSnapshotMetaData(person1Id, p1Snapshot1Version, p1Snapshot1Date)
  val p1Snapshot1Data = ObjectValue("vid", Map("value" -> DoubleValue("0:1", 1)))
  val p1Snapshot1 = ModelSnapshot(p1Snapshot1MetaData, p1Snapshot1Data)

  val inbetweenVersion = 5L

  val p1Snapshot10Version = 10L
  val p1Snapshot10Date = Instant.parse("2016-01-10T00:00:00Z")
  val p1Snapshot10MetaData = ModelSnapshotMetaData(person1Id, p1Snapshot10Version, p1Snapshot10Date)
  val p1Snapshot10Data = ObjectValue("vid", Map("value" -> DoubleValue("0:1", 10)))
  val p1Snapshot10 = ModelSnapshot(p1Snapshot10MetaData, p1Snapshot10Data)

  val p1Snapshot20Version = 20L
  val p1Snapshot20Date = Instant.parse("2016-01-20T00:00:00Z")
  val p1Snapshot20MetaData = ModelSnapshotMetaData(person1Id, p1Snapshot20Version, p1Snapshot20Date)
  val p1Snapshot20Data = ObjectValue("vid", Map("value" -> DoubleValue("0:1", 20)))
  val p1Snapshot20 = ModelSnapshot(p1Snapshot20MetaData, p1Snapshot20Data)
  
  val person2Id = "person2"
  val person2ModelData = ObjectValue("vid", Map("value" -> DoubleValue("0:1", 1)))
  val person2ModelMetaData = ModelMetaData(person2Id, CollectionId, 10, Instant.now(), Instant.now(), true, modelPermissions, 1)
  val person2Model = Model(person2ModelMetaData, person2ModelData)
  
  val p2Snapshot1Version = 20L
  val p2Snapshot1Date = Instant.parse("2016-01-20T00:00:00Z")
  val p2Snapshot1MetaData = ModelSnapshotMetaData(person2Id, 1L, Instant.now())
  val p2Snapshot1Data = ObjectValue("vid", Map("value" -> DoubleValue("0:1", 1)))
  val p2Snapshot1 = ModelSnapshot(p2Snapshot1MetaData, p2Snapshot1Data)
  
  val noPersonId = "noPerson"

  "A ModelSnapshotStore" when {
    "creating a snapshot" must {
      "be able to get the snapshot that was created" in withPersistenceStore { provider =>
        initModels(provider)
        
        val version = 5L
        val timestamp = Date.from(Instant.now()).toInstant

        val created = ModelSnapshot(
          ModelSnapshotMetaData(person1Id, version, timestamp),
          ObjectValue("0:0", Map("key" -> StringValue("0:1", "value"))))

        provider.modelSnapshotStore.createSnapshot(created).success

        val queried = provider.modelSnapshotStore.getSnapshot(person1Id, version)
        queried.get.value shouldBe created
      }
    }

    "when getting a specific snapshot" must {
      "return the correct snapshot when one exists" in withPersistenceStore { provider =>
        createSnapshots(provider)
        val queried = provider.modelSnapshotStore.getSnapshot(person1Id, 1L).get
        queried.value shouldBe p1Snapshot1
      }

      "return None when the specified version does not exist" in withPersistenceStore { provider =>
        createSnapshots(provider)
        val queried = provider.modelSnapshotStore.getSnapshot(person1Id, 5L).get
        queried shouldBe None
      }

      "return None when the specified model does not exist" in withPersistenceStore { provider =>
        createSnapshots(provider)
        val queried = provider.modelSnapshotStore.getSnapshot(noPersonId, 1L).get
        queried shouldBe None
      }
    }

    "when getting all snapshot meta data" must {
      "return all meta data in proper order when no limit or offest is provided" in withPersistenceStore { provider =>
        createSnapshots(provider)
        val metaData = provider.modelSnapshotStore.getSnapshotMetaDataForModel(person1Id, None, None).get
        metaData.length shouldBe 3
        metaData(0).version shouldBe p1Snapshot1Version
        metaData(1).version shouldBe p1Snapshot10Version
        metaData(2).version shouldBe p1Snapshot20Version
      }

      "correctly limit the number of results with no offset" in withPersistenceStore { provider =>
        createSnapshots(provider)
        val metaData = provider.modelSnapshotStore.getSnapshotMetaDataForModel(person1Id, Some(2), None).get
        metaData.length shouldBe 2
        metaData(0).version shouldBe p1Snapshot1Version
        metaData(1).version shouldBe p1Snapshot10Version
      }

      "correctly limit the number of results with an offset" in withPersistenceStore { provider =>
        createSnapshots(provider)
        val metaData = provider.modelSnapshotStore.getSnapshotMetaDataForModel(person1Id, Some(2), Some(1)).get
        metaData.length shouldBe 2
        metaData(0).version shouldBe p1Snapshot10Version
        metaData(1).version shouldBe p1Snapshot20Version
      }

      "correctly offset the results with no limit" in withPersistenceStore { provider =>
        createSnapshots(provider)
        val metaData = provider.modelSnapshotStore.getSnapshotMetaDataForModel(person1Id, None, Some(1)).get
        metaData.length shouldBe 2
        metaData(0).version shouldBe p1Snapshot10Version
        metaData(1).version shouldBe p1Snapshot20Version
      }

      "return an empty list for a non-existent model" in withPersistenceStore { provider =>
        createSnapshots(provider)
        val metaData = provider.modelSnapshotStore.getSnapshotMetaDataForModel(noPersonId, None, None).get
        metaData shouldBe List()
      }
    }

    "when getting snapshots by time" must {
      "return all snapshots if no time or limit-offset" in withPersistenceStore { provider =>
        createSnapshots(provider)
        val metaDataList = provider.modelSnapshotStore.getSnapshotMetaDataForModelByTime(person1Id, None, None, None, None).get
        metaDataList.length shouldBe 3
        metaDataList(0).version shouldBe p1Snapshot1Version
        metaDataList(1).version shouldBe p1Snapshot10Version
        metaDataList(2).version shouldBe p1Snapshot20Version
      }

      "return all snapshots with all encopmasing time bounds and no limit-offset" in withPersistenceStore { provider =>
        createSnapshots(provider)
        val metaDataList = provider.modelSnapshotStore.getSnapshotMetaDataForModelByTime(
          person1Id, Some(p1Snapshot1Date.toEpochMilli), Some(p1Snapshot20Date.toEpochMilli), None, None).get
        metaDataList.length shouldBe 3
        metaDataList(0).version shouldBe p1Snapshot1Version
        metaDataList(1).version shouldBe p1Snapshot10Version
        metaDataList(2).version shouldBe p1Snapshot20Version
      }
    }

    "when getting the latest snapshot for a model" must {
      "return the correct meta data for a model with snapshots" in withPersistenceStore { provider =>
        createSnapshots(provider)
        val metaData = provider.modelSnapshotStore.getLatestSnapshotMetaDataForModel(person1Id).get
        metaData.value.version shouldBe p1Snapshot20Version
      }

      "return None when the specified model does not exist" in withPersistenceStore { provider =>
        createSnapshots(provider)
        val queried = provider.modelSnapshotStore.getSnapshot(noPersonId, 1L).get
        queried shouldBe None
      }
    }

    "when getting the closest snapshot to a version for a model" must {
      "return the higher version when it is the closest" in withPersistenceStore { provider =>
        createSnapshots(provider)
        val snapshotData = provider.modelSnapshotStore.getClosestSnapshotByVersion(person1Id, 18).get
        snapshotData.value.metaData.version shouldBe p1Snapshot20Version
      }

      "return the lower version when it is the closest" in withPersistenceStore { provider =>
        createSnapshots(provider)
        val snapshotData = provider.modelSnapshotStore.getClosestSnapshotByVersion(person1Id, 14).get
        snapshotData.value.metaData.version shouldBe p1Snapshot10Version
      }

      "return the higher version when the requested version is equidistant from a higerh and lower snapshot" in withPersistenceStore { provider =>
        createSnapshots(provider)
        val snapshotData = provider.modelSnapshotStore.getClosestSnapshotByVersion(person1Id, 15).get
        snapshotData.value.metaData.version shouldBe p1Snapshot20Version
      }
    }

    "when removing a single snapshot by model and version" must {
      "remove the specified snapshot and no others" in withPersistenceStore { provider =>
        createSnapshots(provider)
        provider.modelSnapshotStore.getSnapshot(person1Id, p1Snapshot1Version).get shouldBe defined
        provider.modelSnapshotStore.removeSnapshot(person1Id, p1Snapshot1Version)
        provider.modelSnapshotStore.getSnapshot(person1Id, p1Snapshot1Version).get shouldBe None

        // Ensure no others were deleted from the desired model
        val person1MetaData = provider.modelSnapshotStore.getSnapshotMetaDataForModel(person1Id, None, None).get
        person1MetaData.length shouldBe 2

        person1MetaData(0).version shouldBe p1Snapshot10Version
        person1MetaData(1).version shouldBe p1Snapshot20Version

        // Ensure no others were deleted from the other model
        val person2MetaData = provider.modelSnapshotStore.getSnapshotMetaDataForModel(person2Id, None, None).get
        person2MetaData.length shouldBe 1
      }
    }

    "when removing all snapshots by model and version" must {
      "remove the snapshots for the specified model and no others" in withPersistenceStore { provider =>
        createSnapshots(provider)
        provider.modelSnapshotStore.getSnapshotMetaDataForModel(person1Id, None, None).get.length shouldBe 3
        provider.modelSnapshotStore.removeAllSnapshotsForModel(person1Id)
        provider.modelSnapshotStore.getSnapshotMetaDataForModel(person1Id, None, None).get.length shouldBe 0

        // Ensure no others were deleted from the other model
        provider.modelSnapshotStore.getSnapshotMetaDataForModel(person2Id, None, None).get.length shouldBe 1
      }
    }

    "when removing all snapshots" must {
      "remove all snapshots for all models in a collection" in withPersistenceStore { provider =>
        createSnapshots(provider)
        
        provider.modelSnapshotStore.getSnapshotMetaDataForModel(person1Id, None, None).get.length shouldBe 3
        provider.modelSnapshotStore.getSnapshotMetaDataForModel(person2Id, None, None).get.length shouldBe 1

        provider.modelSnapshotStore.removeAllSnapshotsForCollection(CollectionId).success

        provider.modelSnapshotStore.getSnapshotMetaDataForModel(person1Id, None, None).get.length shouldBe 0
        provider.modelSnapshotStore.getSnapshotMetaDataForModel(person2Id, None, None).get.length shouldBe 0
      }
    }
  }

  def initModels(provider: DomainPersistenceProvider): Unit = {
    provider.collectionStore.ensureCollectionExists(CollectionId).get
    provider.modelStore.createModel(person1Model).get
    provider.modelStore.createModel(person2Model).get
  }

  def createSnapshots(provider: DomainPersistenceProvider): Unit = {
    initModels(provider)
    provider.modelSnapshotStore.createSnapshot(p1Snapshot1).get
    provider.modelSnapshotStore.createSnapshot(p1Snapshot10).get
    provider.modelSnapshotStore.createSnapshot(p1Snapshot20).get
    provider.modelSnapshotStore.createSnapshot(p2Snapshot1).get
  }
}
