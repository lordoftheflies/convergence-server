package com.convergencelabs.server.datastore.domain.mapper

import java.time.Instant
import org.json4s.JsonAST.JObject
import org.json4s.JsonAST.JString
import org.scalatest.Matchers
import org.scalatest.WordSpec
import com.convergencelabs.server.domain.model.Model
import com.convergencelabs.server.domain.model.ModelFqn
import com.convergencelabs.server.domain.model.ModelMetaData
import com.orientechnologies.orient.core.record.impl.ODocument
import ModelMapper.ODocumentToModel
import ModelMapper.ODocumentToModelMetaData
import com.convergencelabs.server.domain.model.data.ObjectValue
import com.convergencelabs.server.domain.model.data.StringValue

class ModelMapperSpec
    extends WordSpec
    with Matchers {

  val ModelVersion = 4L

  "An ModelMapper" when {
    "when converting Model operations" must {
      "correctly map and unmap a Model" in {
//        val model = Model(
//          ModelMetaData(
//            ModelFqn("collection", "model"),
//            ModelVersion,
//            Instant.ofEpochMilli(System.currentTimeMillis()),
//            Instant.ofEpochMilli(System.currentTimeMillis())),
//          ObjectValue("vid1", Map("foo" -> StringValue("vid2", "test"))))
//
//        val opDoc = model.asODocument
//        val reverted = opDoc.asModel
//        reverted shouldBe model
      }

      "correctly map an ODoducment to ModeMetaData" in {
//        val metaData = ModelMetaData(
//          ModelFqn("collection", "model"),
//          ModelVersion,
//          Instant.ofEpochMilli(System.currentTimeMillis()),
//          Instant.ofEpochMilli(System.currentTimeMillis()))
//
//        val model = Model(
//          metaData,
//          JObject("foo" -> JString("test")))
//
//        val opDoc = model.asODocument
//        val reverted = opDoc.asModelMetaData
//        reverted shouldBe metaData
      }

      "not allow an invalid document class name" in {
//        val invalid = new ODocument("SomeClass")
//        intercept[IllegalArgumentException] {
//          invalid.asModel
//        }
      }
    }
  }
}