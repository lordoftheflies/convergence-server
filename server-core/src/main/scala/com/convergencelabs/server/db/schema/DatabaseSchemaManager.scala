package com.convergencelabs.server.db.schema

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import org.json4s.DefaultFormats
import org.json4s.ext.EnumNameSerializer
import org.json4s.ShortTypeHints
import java.io.File
import org.json4s.jackson.JsonMethods
import org.json4s.Extraction
import com.orientechnologies.orient.core.metadata.schema.OClass
import org.json4s.FieldSerializer
import scala.io.Source
import java.io.FileNotFoundException
import scala.util.Try
import scala.util.Success
import scala.util.Failure
import com.orientechnologies.orient.core.db.OPartitionedDatabasePool
import java.io.InputStream
import grizzled.slf4j.Logging
import com.convergencelabs.server.util.SimpleNamePolymorphicSerializer

object DatabaseSchemaManager {
  val DatabaseVersion = "DatabaseVersion"

  object Fields {
    val Version = "version"
    val ManagerVersion = "managerVersion"
  }
}

class DatabaseSchemaManager(dbPool: OPartitionedDatabasePool, category: DeltaCategory.Value) extends Logging {

  private[this] implicit val releaseOnly = true

  private[this] val mapper = new ObjectMapper(new YAMLFactory())
  private[this] implicit val format = DefaultFormats +
    SimpleNamePolymorphicSerializer[Change]("action", List(
      classOf[CreateClass],
      classOf[AlterClass],
      classOf[DropClass],
      classOf[AddProperty],
      classOf[AlterProperty],
      classOf[DropProperty],
      classOf[CreateIndex],
      classOf[DropIndex],
      classOf[CreateSequence],
      classOf[DropSequence],
      classOf[RunSQLCommand],
      classOf[CreateFunction],
      classOf[AlterFunction],
      classOf[DropFunction])) +
    new EnumNameSerializer(OrientType) +
    new EnumNameSerializer(IndexType) +
    new EnumNameSerializer(SequenceType)

  private[this] var versionController = new DatabaseVersionController(dbPool)
  private[this] val processor = new DatabaseSchemaProcessor(dbPool)
  private[this] val deltaManager = new DeltaManager(None)

  def currentVersion(): Try[Int] = {
    versionController.getVersion()
  }

  private[this] def currentManagerVersion(): Try[Int] = {
    versionController.getManagerVersion()
  }

  def upgradeToVersion(version: Int): Try[Unit] = {
    deltaManager.manifest(category) match {
      case Success(manifest) => upgrade(manifest, version)
      case Failure(e) =>
        logger.error("Unable to load manifest")
        Failure(e)
    }
  }

  def upgradeToLatest(): Try[Unit] = {
    deltaManager.manifest(category) match {
      case Success(manifest) => upgrade(manifest, manifest.maxReleasedDelta())
      case Failure(e) =>
        logger.error("Unable to load manifest")
        Failure(e)
    }
  }

  private[this] def upgrade(manifest: DeltaManifest, version: Int): Try[Unit] = Try {
    upgradeManagerVersion()
    currentVersion() match {
      case Success(currentVersion) =>
        manifest.forEach(true)((deltaNumber: Int, path: String, in: InputStream) => {
          if (deltaNumber > currentVersion && deltaNumber <= version) {
            getDelta(in) match {
              case Success(delta) =>
                processor.applyDelta(delta)
                versionController.setVersion(deltaNumber)
              case Failure(e) =>
                logger.error("Unable to apply manager deltas")
                Failure(e)
            }
          }
          Success(())
        })
        Success(())
      case Failure(e) =>
        logger.error("Unable to lookup current version for database")
        Failure(e)
    }
  }

  private[this] def upgradeManagerVersion(): Try[Unit] = {
    currentManagerVersion() match {
      case Success(currentVersion) =>
        if (currentVersion < DatabaseVersionController.ManagerVersion) {
          logger.info(s"Manager schema is out of date.  Upgrading manager schema to version: ${DatabaseVersionController.ManagerVersion}")
          deltaManager.manifest(DeltaCategory.Version) match {
            case Success(manifest: DeltaManifest) =>
              logger.info("Found manifest.  Applying deltas...")
              Try {

                manifest.forEach(true)((deltaNumber, path, in) => {
                  if (deltaNumber > currentVersion && deltaNumber <= DatabaseVersionController.ManagerVersion) {
                    getDelta(in) match {
                      case Success(delta) =>
                        logger.info(s"Applying delta: $deltaNumber")
                        processor.applyDelta(delta) match {
                          case Success(()) => versionController.setManagerVersion(deltaNumber)
                          case Failure(e) =>
                            logger.error("Unable to apply manager deltas")
                            Failure(e)
                        }
                      case Failure(e) =>
                        logger.error("Unable to read delta")
                        Failure(e)
                    }
                  }
                  Success(())
                })
              }
            case Failure(e) =>
              logger.error("Unable to load manifest for manager")
              Failure(e)
          }
        } else {
          Success(())
        }
      case Failure(e) =>
        logger.error("Unable to lookup current manager version for database")
        Failure(e)
    }
  }

  private[this] def getDelta(in: InputStream): Try[Delta] = Try {
    val jsonNode = mapper.readTree(in)
    val jValue = JsonMethods.fromJsonNode(jsonNode)
    Extraction.extract[Delta](jValue)
  }
}