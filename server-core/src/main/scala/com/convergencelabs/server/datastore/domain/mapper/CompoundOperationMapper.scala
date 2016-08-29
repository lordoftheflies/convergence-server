package com.convergencelabs.server.datastore.domain.mapper

import java.util.{ List => JavaList }

import scala.collection.JavaConverters.asScalaBufferConverter
import scala.collection.JavaConverters.seqAsJavaListConverter
import scala.language.implicitConversions

import com.convergencelabs.server.datastore.mapper.ODocumentMapper
import com.convergencelabs.server.domain.model.ot.CompoundOperation
import com.orientechnologies.orient.core.metadata.schema.OType
import com.orientechnologies.orient.core.record.impl.ODocument

object CompoundOperationMapper extends ODocumentMapper {

  private[domain] implicit class CompoundOperationToODocument(val s: CompoundOperation) extends AnyVal {
    def asODocument: ODocument = compoundOperationToODocument(s)
  }

  private[domain] implicit def compoundOperationToODocument(obj: CompoundOperation): ODocument = {
    val CompoundOperation(ops) = obj
    val doc = new ODocument(DocumentClassName)
    val opDocs = ops.map { OrientDBOperationMapper.operationToODocument(_) }
    doc.field(Fields.Ops, opDocs.asJava, OType.EMBEDDEDLIST)
    doc
  }

  private[domain] implicit class ODocumentToCompoundOperation(val d: ODocument) extends AnyVal {
    def asCompoundOperation: CompoundOperation = oDocumentToCompoundOperation(d)
  }

  private[domain] implicit def oDocumentToCompoundOperation(doc: ODocument): CompoundOperation = {
    validateDocumentClass(doc, DocumentClassName)

    val opDocs: JavaList[ODocument] = doc.field(Fields.Ops, OType.EMBEDDEDLIST)
    val ops = opDocs.asScala.toList.map { OrientDBOperationMapper.oDocumentToDiscreteOperation(_) }
    CompoundOperation(ops)
  }

  private[domain] val DocumentClassName = "CompoundOperation"

  private[domain] object Fields {
    val Ops = "ops"
  }
}