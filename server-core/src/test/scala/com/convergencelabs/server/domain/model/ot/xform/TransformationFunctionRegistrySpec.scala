package com.convergencelabs.server.domain.model.ot

import scala.math.BigInt.int2bigInt
import org.json4s.JsonAST.JArray
import org.json4s.JsonAST.JInt
import org.json4s.JsonAST.JObject
import org.json4s.JsonAST.JString
import org.scalatest.Finders
import org.scalatest.Matchers
import org.scalatest.OptionValues.convertOptionToValuable
import org.scalatest.WordSpec
import org.json4s.JsonAST.JDouble
import com.convergencelabs.server.domain.model.data.StringValue
import com.convergencelabs.server.domain.model.ReferenceType
import com.convergencelabs.server.domain.model.SetReference
import com.convergencelabs.server.domain.model.ot.xform.reference.StringInsertIndexTF
import com.convergencelabs.server.domain.model.ot.xform.reference.StringRemoveIndexTF
import com.convergencelabs.server.domain.model.ot.xform.reference.StringSetIndexTF
import com.convergencelabs.server.domain.model.ot.xform.reference.StringInsertRangeTF
import com.convergencelabs.server.domain.model.ot.xform.reference.StringRemoveRangeTF
import com.convergencelabs.server.domain.model.ot.xform.reference.StringSetRangeTF

// scalastyle:off multiple.string.literals
class TransformationFunctionRegistrySpec extends WordSpec with Matchers {

  val valueId = "testId"

  val StringInsert = StringInsertOperation(valueId, false, 1, "")
  val StringRemove = StringRemoveOperation(valueId, false, 1, "")
  val StringSet = StringSetOperation(valueId, false, "4")

  val ArrayInsert = ArrayInsertOperation(valueId, false, 1, StringValue("id", "4"))
  val ArrayRemove = ArrayRemoveOperation(valueId, false, 1)
  val ArrayReplace = ArrayReplaceOperation(valueId, false, 1, StringValue("id", "4"))
  val ArrayMove = ArrayMoveOperation(valueId, false, 1, 1)
  val ArraySet = ArraySetOperation(valueId, false, List(StringValue("id", "4")))

  val ObjectAddProperty = ObjectAddPropertyOperation(valueId, false, "prop", StringValue("id", "4"))
  val ObjectSetProperty = ObjectSetPropertyOperation(valueId, false, "prop", StringValue("id", "4"))
  val ObjectRemoveProperty = ObjectRemovePropertyOperation(valueId, false, "prop")
  val ObjectSet = ObjectSetOperation(valueId, false, Map())

  val NumberAdd = NumberAddOperation(valueId, false, 1d)
  val NumberSet = NumberSetOperation(valueId, false, 1d)

  val BooleanSet = BooleanSetOperation(valueId, false, true)
  
  val referenceKey = "refKey"
  
  val SetRef = SetReference(Some(valueId), referenceKey, ReferenceType.Index, List(3), 1l)

  "A TransformationFunctionRegistry" when {

    ///////////////////////////////////////////////////////////////////////////
    // String Operations
    ///////////////////////////////////////////////////////////////////////////

    "getting a TransformationFunction for a StringInsertOperation and anoter StringOperation" must {
      "return the StringInsertInsertTF when a StringInsertOperation and a StringInsertOperation are passed in" in {
        val tfr = new TransformationFunctionRegistry()
        val tf = tfr.getOperationTransformationFunction(StringInsert, StringInsert)
        tf.value shouldBe StringInsertInsertTF
      }

      "return the StringInsertInsertTF when a StringInsertOperation and a StringRemoveOperation are passed in" in {
        val tfr = new TransformationFunctionRegistry()
        val tf = tfr.getOperationTransformationFunction(StringInsert, StringRemove)
        tf.value shouldBe StringInsertRemoveTF
      }

      "return the StringInsertInsertTF when a StringInsertOperation and a StringSetOperation are passed in" in {
        val tfr = new TransformationFunctionRegistry()
        val tf = tfr.getOperationTransformationFunction(StringInsert, StringSet)
        tf.value shouldBe StringInsertSetTF
      }
    }

    "getting a TransformationFunction for a StringRemoveOperation and anoter StringOperation" must {
      "return the StringInsertInsertTF when a StringRemoveOperation and a StringInsertOperation are passed in" in {
        val tfr = new TransformationFunctionRegistry()
        val tf = tfr.getOperationTransformationFunction(StringRemove, StringInsert)
        tf.value shouldBe StringRemoveInsertTF
      }

      "return the StringRemoveRemoveTF when a StringRemoveOperation and a StringRemoveOperation are passed in" in {
        val tfr = new TransformationFunctionRegistry()
        val tf = tfr.getOperationTransformationFunction(StringRemove, StringRemove)
        tf.value shouldBe StringRemoveRemoveTF
      }

      "return the StringRemoveRemoveTF when a StringRemoveOperation and a StringSetOperation are passed in" in {
        val tfr = new TransformationFunctionRegistry()
        val tf = tfr.getOperationTransformationFunction(StringRemove, StringSet)
        tf.value shouldBe StringRemoveSetTF
      }
    }

    "getting a TransformationFunction for a StringSetOperation and anoter StringOperation" must {
      "return the StringInsertInsertTF when a StringSetOperation and a StringInsertOperation are passed in" in {
        val tfr = new TransformationFunctionRegistry()
        val tf = tfr.getOperationTransformationFunction(StringSet, StringInsert)
        tf.value shouldBe StringSetInsertTF
      }

      "return the StringSetSetTF when a StringSetOperation and a StringRemoveOperation are passed in" in {
        val tfr = new TransformationFunctionRegistry()
        val tf = tfr.getOperationTransformationFunction(StringSet, StringRemove)
        tf.value shouldBe StringSetRemoveTF
      }

      "return the StringSetSetTF when a StringSetOperation and a StringSetOperation are passed in" in {
        val tfr = new TransformationFunctionRegistry()
        val tf = tfr.getOperationTransformationFunction(StringSet, StringSet)
        tf.value shouldBe StringSetSetTF
      }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Array Operations
    ///////////////////////////////////////////////////////////////////////////

    "getting a TransformationFunction for an ArrayInsertOperation and anoter ArrayOperation" must {
      "return the ArrayInsertInsertTF when an ArrayInsertOperation and an ArrayInsertOpertion are passed in" in {
        val tfr = new TransformationFunctionRegistry()
        val tf = tfr.getOperationTransformationFunction(ArrayInsert, ArrayInsert)
        tf.value shouldBe ArrayInsertInsertTF
      }

      "return the ArrayInsertRemoveTF when an ArrayInsertOperation and an ArrayRemoveOpertion are passed in" in {
        val tfr = new TransformationFunctionRegistry()
        val tf = tfr.getOperationTransformationFunction(ArrayInsert, ArrayRemove)
        tf.value shouldBe ArrayInsertRemoveTF
      }

      "return the ArrayInsertReplaceTF when an ArrayInsertOperation and an ArrayReplaceOpertion are passed in" in {
        val tfr = new TransformationFunctionRegistry()
        val tf = tfr.getOperationTransformationFunction(ArrayInsert, ArrayReplace)
        tf.value shouldBe ArrayInsertReplaceTF
      }

      "return the ArrayInsertMoveTF when an ArrayInsertOperation and an ArrayMoveOpertion are passed in" in {
        val tfr = new TransformationFunctionRegistry()
        val tf = tfr.getOperationTransformationFunction(ArrayInsert, ArrayMove)
        tf.value shouldBe ArrayInsertMoveTF
      }

      "return the ArrayInsertSetTF when an ArrayInsertOperation and an ArraySetOpertion are passed in" in {
        val tfr = new TransformationFunctionRegistry()
        val tf = tfr.getOperationTransformationFunction(ArrayInsert, ArraySet)
        tf.value shouldBe ArrayInsertSetTF
      }
    }

    "getting a TransformationFunction for an ArrayRemoveOperation and anoter ArrayOperation" must {
      "return the ArrayRemoveInsertTF when an ArrayRemoveOperation and an ArrayInsertOpertion are passed in" in {
        val tfr = new TransformationFunctionRegistry()
        val tf = tfr.getOperationTransformationFunction(ArrayRemove, ArrayInsert)
        tf.value shouldBe ArrayRemoveInsertTF
      }

      "return the ArrayRemoveRemoveTF when an ArrayRemoveOperation and an ArrayRemoveOpertion are passed in" in {
        val tfr = new TransformationFunctionRegistry()
        val tf = tfr.getOperationTransformationFunction(ArrayRemove, ArrayRemove)
        tf.value shouldBe ArrayRemoveRemoveTF
      }

      "return the ArrayRemoveReplaceTF when an ArrayRemoveOperation and an ArrayReplaceOpertion are passed in" in {
        val tfr = new TransformationFunctionRegistry()
        val tf = tfr.getOperationTransformationFunction(ArrayRemove, ArrayReplace)
        tf.value shouldBe ArrayRemoveReplaceTF
      }

      "return the ArrayRemoveMoveTF when an ArrayRemoveOperation and an ArrayMoveOpertion are passed in" in {
        val tfr = new TransformationFunctionRegistry()
        val tf = tfr.getOperationTransformationFunction(ArrayRemove, ArrayMove)
        tf.value shouldBe ArrayRemoveMoveTF
      }

      "return the ArrayRemoveSetTF when an ArrayRemoveOperation and an ArraySetOpertion are passed in" in {
        val tfr = new TransformationFunctionRegistry()
        val tf = tfr.getOperationTransformationFunction(ArrayRemove, ArraySet)
        tf.value shouldBe ArrayRemoveSetTF
      }
    }

    "getting a TransformationFunction for an ArrayReplaceOperation and anoter ArrayOperation" must {
      "return the ArrayInsertInsertTF when an ArrayReplaceOperation and an ArrayInsertOpertion are passed in" in {
        val tfr = new TransformationFunctionRegistry()
        val tf = tfr.getOperationTransformationFunction(ArrayReplace, ArrayInsert)
        tf.value shouldBe ArrayReplaceInsertTF
      }

      "return the ArrayReplaceRemoveTF when an ArrayReplaceOperation and an ArrayRemoveOpertion are passed in" in {
        val tfr = new TransformationFunctionRegistry()
        val tf = tfr.getOperationTransformationFunction(ArrayReplace, ArrayRemove)
        tf.value shouldBe ArrayReplaceRemoveTF
      }

      "return the ArrayReplaceReplaceTF when an ArrayReplaceOperation and an ArrayReplaceOpertion are passed in" in {
        val tfr = new TransformationFunctionRegistry()
        val tf = tfr.getOperationTransformationFunction(ArrayReplace, ArrayReplace)
        tf.value shouldBe ArrayReplaceReplaceTF
      }

      "return the ArrayReplaceMoveTF when an ArrayReplaceOperation and an ArrayMoveOpertion are passed in" in {
        val tfr = new TransformationFunctionRegistry()
        val tf = tfr.getOperationTransformationFunction(ArrayReplace, ArrayMove)
        tf.value shouldBe ArrayReplaceMoveTF
      }

      "return the ArrayReplaceSetTF when an ArrayReplaceOperation and an ArraySetOpertion are passed in" in {
        val tfr = new TransformationFunctionRegistry()
        val tf = tfr.getOperationTransformationFunction(ArrayReplace, ArraySet)
        tf.value shouldBe ArrayReplaceSetTF
      }
    }

    "getting a TransformationFunction for an ArrayMoveOperation and anoter ArrayOperation" must {
      "return the ArrayInsertInsertTF when an ArrayMoveOperation and an ArrayInsertOpertion are passed in" in {
        val tfr = new TransformationFunctionRegistry()
        val tf = tfr.getOperationTransformationFunction(ArrayMove, ArrayInsert)
        tf.value shouldBe ArrayMoveInsertTF
      }

      "return the ArrayMoveRemoveTF when an ArrayMoveOperation and an ArrayRemoveOpertion are passed in" in {
        val tfr = new TransformationFunctionRegistry()
        val tf = tfr.getOperationTransformationFunction(ArrayMove, ArrayRemove)
        tf.value shouldBe ArrayMoveRemoveTF
      }

      "return the ArrayMoveReplaceTF when an ArrayMoveOperation and an ArrayReplaceOpertion are passed in" in {
        val tfr = new TransformationFunctionRegistry()
        val tf = tfr.getOperationTransformationFunction(ArrayMove, ArrayReplace)
        tf.value shouldBe ArrayMoveReplaceTF
      }

      "return the ArrayMoveMoveTF when an ArrayMoveOperation and an ArrayMoveOpertion are passed in" in {
        val tfr = new TransformationFunctionRegistry()
        val tf = tfr.getOperationTransformationFunction(ArrayMove, ArrayMove)
        tf.value shouldBe ArrayMoveMoveTF
      }

      "return the ArrayMoveSetTF when an ArrayMoveOperation and an ArraySetOpertion are passed in" in {
        val tfr = new TransformationFunctionRegistry()
        val tf = tfr.getOperationTransformationFunction(ArrayMove, ArraySet)
        tf.value shouldBe ArrayMoveSetTF
      }
    }

    "getting a TransformationFunction for an ArraySetOperation and anoter ArrayOperation" must {
      "return the ArraySetInsertTF when an ArraySetOperation and an ArrayInsertOpertion are passed in" in {
        val tfr = new TransformationFunctionRegistry()
        val tf = tfr.getOperationTransformationFunction(ArraySet, ArrayInsert)
        tf.value shouldBe ArraySetInsertTF
      }

      "return the ArraySetRemoveTF when an ArraySetOperation and an ArrayRemoveOpertion are passed in" in {
        val tfr = new TransformationFunctionRegistry()
        val tf = tfr.getOperationTransformationFunction(ArraySet, ArrayRemove)
        tf.value shouldBe ArraySetRemoveTF
      }

      "return the ArraySetReplaceTF when an ArraySetOperation and an ArrayReplaceOpertion are passed in" in {
        val tfr = new TransformationFunctionRegistry()
        val tf = tfr.getOperationTransformationFunction(ArraySet, ArrayReplace)
        tf.value shouldBe ArraySetReplaceTF
      }

      "return the ArraySetMoveTF when an ArraySetOperation and an ArrayMoveOpertion are passed in" in {
        val tfr = new TransformationFunctionRegistry()
        val tf = tfr.getOperationTransformationFunction(ArraySet, ArrayMove)
        tf.value shouldBe ArraySetMoveTF
      }

      "return the ArraySetSetTF when an ArraySetOperation and an ArraySetOpertion are passed in" in {
        val tfr = new TransformationFunctionRegistry()
        val tf = tfr.getOperationTransformationFunction(ArraySet, ArraySet)
        tf.value shouldBe ArraySetSetTF
      }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Object Operations
    ///////////////////////////////////////////////////////////////////////////

    "getting a TransformationFunction for an ObjectSetPropertyOperation and anoter ObjectOperation" must {
      "return the ObjectSetPropertySetPropertyTF when an ObjectSetPropertyOperation and an ObjectSetPropertyOperation are passed in" in {
        val tfr = new TransformationFunctionRegistry()
        val tf = tfr.getOperationTransformationFunction(ObjectSetProperty, ObjectSetProperty)
        tf.value shouldBe ObjectSetPropertySetPropertyTF
      }

      "return the ObjectSetPropertyAddPropertyTF when an ObjectSetPropertyOperation and an ObjectAddPropertyOperation are passed in" in {
        val tfr = new TransformationFunctionRegistry()
        val tf = tfr.getOperationTransformationFunction(ObjectSetProperty, ObjectAddProperty)
        tf.value shouldBe ObjectSetPropertyAddPropertyTF
      }

      "return the ObjectSetPropertyRemovePropertyTF when an ObjectSetPropertyOperation and an ObjectRemovePropertyOperation are passed in" in {
        val tfr = new TransformationFunctionRegistry()
        val tf = tfr.getOperationTransformationFunction(ObjectSetProperty, ObjectRemoveProperty)
        tf.value shouldBe ObjectSetPropertyRemovePropertyTF
      }

      "return the ObjectSetPropertyRemovePropertyTF when an ObjectSetPropertyOperation and an ObjectSetOperation are passed in" in {
        val tfr = new TransformationFunctionRegistry()
        val tf = tfr.getOperationTransformationFunction(ObjectSetProperty, ObjectSet)
        tf.value shouldBe ObjectSetPropertySetTF
      }
    }

    "getting a TransformationFunction for an ObjectAddPropertyOperation and anoter ObjectOperation" must {
      "return the ObjectAddPropertySetPropertyTF when an ObjectAddPropertyOperation and an ObjectSetPropertyOperation are passed in" in {
        val tfr = new TransformationFunctionRegistry()
        val tf = tfr.getOperationTransformationFunction(ObjectAddProperty, ObjectSetProperty)
        tf.value shouldBe ObjectAddPropertySetPropertyTF
      }

      "return the ObjectAddPropertyAddPropertyTF when an ObjectAddPropertyOperation and an ObjectAddPropertyOperation are passed in" in {
        val tfr = new TransformationFunctionRegistry()
        val tf = tfr.getOperationTransformationFunction(ObjectAddProperty, ObjectAddProperty)
        tf.value shouldBe ObjectAddPropertyAddPropertyTF
      }

      "return the ObjectAddPropertyRemovePropertyTF when an ObjectAddPropertyOperation and an ObjectRemovePropertyOperation are passed in" in {
        val tfr = new TransformationFunctionRegistry()
        val tf = tfr.getOperationTransformationFunction(ObjectAddProperty, ObjectRemoveProperty)
        tf.value shouldBe ObjectAddPropertyRemovePropertyTF
      }

      "return the ObjectAddPropertyRemovePropertyTF when an ObjectAddPropertyOperation and an ObjectSetOperation are passed in" in {
        val tfr = new TransformationFunctionRegistry()
        val tf = tfr.getOperationTransformationFunction(ObjectAddProperty, ObjectSet)
        tf.value shouldBe ObjectAddPropertySetTF
      }
    }

    "getting a TransformationFunction for an ObjectRemovePropertyOperation and anoter ObjectOperation" must {
      "return the ObjectRemovePropertySetPropertyTF when an ObjectRemovePropertyOperation and an ObjectSetPropertyOperation are passed in" in {
        val tfr = new TransformationFunctionRegistry()
        val tf = tfr.getOperationTransformationFunction(ObjectRemoveProperty, ObjectSetProperty)
        tf.value shouldBe ObjectRemovePropertySetPropertyTF
      }

      "return the ObjectRemovePropertyAddPropertyTF when an ObjectRemovePropertyOperation and an ObjectAddPropertyOperation are passed in" in {
        val tfr = new TransformationFunctionRegistry()
        val tf = tfr.getOperationTransformationFunction(ObjectRemoveProperty, ObjectAddProperty)
        tf.value shouldBe ObjectRemovePropertyAddPropertyTF
      }

      "return the ObjectRemovePropertyRemovePropertyTF when an ObjectRemovePropertyOperation and an ObjectRemovePropertyOperation are passed in" in {
        val tfr = new TransformationFunctionRegistry()
        val tf = tfr.getOperationTransformationFunction(ObjectRemoveProperty, ObjectRemoveProperty)
        tf.value shouldBe ObjectRemovePropertyRemovePropertyTF
      }

      "return the ObjectRemovePropertyRemovePropertyTF when an ObjectRemovePropertyOperation and an ObjectSetOperation are passed in" in {
        val tfr = new TransformationFunctionRegistry()
        val tf = tfr.getOperationTransformationFunction(ObjectRemoveProperty, ObjectSet)
        tf.value shouldBe ObjectRemovePropertySetTF
      }
    }

    "getting a TransformationFunction for an ObjectSetOperation and anoter ObjectOperation" must {
      "return the ObjectSetSetPropertyTF when an ObjectSetOperation and an ObjectSetPropertyOperation are passed in" in {
        val tfr = new TransformationFunctionRegistry()
        val tf = tfr.getOperationTransformationFunction(ObjectSet, ObjectSetProperty)
        tf.value shouldBe ObjectSetSetPropertyTF
      }

      "return the ObjectSetSetTF when an ObjectSetOperation and an ObjectAddPropertyOperation are passed in" in {
        val tfr = new TransformationFunctionRegistry()
        val tf = tfr.getOperationTransformationFunction(ObjectSet, ObjectAddProperty)
        tf.value shouldBe ObjectSetAddPropertyTF
      }

      "return the ObjectSetRemovePropertyTF when an ObjectSetOperation and an ObjectRemovePropertyOperation are passed in" in {
        val tfr = new TransformationFunctionRegistry()
        val tf = tfr.getOperationTransformationFunction(ObjectSet, ObjectRemoveProperty)
        tf.value shouldBe ObjectSetRemovePropertyTF
      }

      "return the ObjectSetRemovePropertyTF when an ObjectSetOperation and an ObjectSetOperation are passed in" in {
        val tfr = new TransformationFunctionRegistry()
        val tf = tfr.getOperationTransformationFunction(ObjectSet, ObjectSet)
        tf.value shouldBe ObjectSetSetTF
      }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Number Operations
    ///////////////////////////////////////////////////////////////////////////

    "getting a TransformationFunction for an NumberAddOperation and anoter NumberOperation" must {
      "return the NumberAddAddTF when a NumberAddOperation and a NumberAddOperation are passed in" in {
        val tfr = new TransformationFunctionRegistry()
        val tf = tfr.getOperationTransformationFunction(NumberAdd, NumberAdd)
        tf.value shouldBe NumberAddAddTF
      }

      "return the NumberAddSetTF when a NumberAddOperation and a NumberSetOperation are passed in" in {
        val tfr = new TransformationFunctionRegistry()
        val tf = tfr.getOperationTransformationFunction(NumberAdd, NumberSet)
        tf.value shouldBe NumberAddSetTF
      }
    }

    "getting a TransformationFunction for an NumberSetOperation and anoter NumberOperation" must {
      "return the NumberSetAddTF when a NumberSetOperation and a NumberAddOperation are passed in" in {
        val tfr = new TransformationFunctionRegistry()
        val tf = tfr.getOperationTransformationFunction(NumberSet, NumberAdd)
        tf.value shouldBe NumberSetAddTF
      }

      "return the NumberSetSetTF when a NumberSetOperation and a NumberSetOperation are passed in" in {
        val tfr = new TransformationFunctionRegistry()
        val tf = tfr.getOperationTransformationFunction(NumberSet, NumberSet)
        tf.value shouldBe NumberSetSetTF
      }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Boolean Operations
    ///////////////////////////////////////////////////////////////////////////

    "getting a TransformationFunction for an BooleanSetOperation and anoter BooleanOperation" must {
      "return the BooleanSetSetTF when a BooleanSetOperation and a BooleanSetOperation are passed in" in {
        val tfr = new TransformationFunctionRegistry()
        val tf = tfr.getOperationTransformationFunction(BooleanSet, BooleanSet)
        tf.value shouldBe BooleanSetSetTF
      }
    }
    
    ///////////////////////////////////////////////////////////////////////////
    // String References
    ///////////////////////////////////////////////////////////////////////////
    "getting a ReferenceTransformationFunction for an StringInsert and an Index reference" must {
      "return StringInsertIndexFT" in {
        val tfr = new TransformationFunctionRegistry()
        val tf = tfr.getReferenceTransformationFunction(StringInsert, ReferenceType.Index)
        tf.value shouldBe StringInsertIndexTF
      }
    }
    
    "getting a ReferenceTransformationFunction for an StringRemove and an Index reference" must {
      "return StringRemoveIndexTF" in {
        val tfr = new TransformationFunctionRegistry()
        val tf = tfr.getReferenceTransformationFunction(StringRemove, ReferenceType.Index)
        tf.value shouldBe StringRemoveIndexTF
      }
    }
    
    "getting a ReferenceTransformationFunction for an StringSet and an Index reference" must {
      "return StringSetIndexTF" in {
        val tfr = new TransformationFunctionRegistry()
        val tf = tfr.getReferenceTransformationFunction(StringSet, ReferenceType.Index)
        tf.value shouldBe StringSetIndexTF
      }
    }
    
    "getting a ReferenceTransformationFunction for an StringInsert and an Range reference" must {
      "return StringInsertIndexFT" in {
        val tfr = new TransformationFunctionRegistry()
        val tf = tfr.getReferenceTransformationFunction(StringInsert, ReferenceType.Range)
        tf.value shouldBe StringInsertRangeTF
      }
    }
    
    "getting a ReferenceTransformationFunction for an StringRemove and an Range reference" must {
      "return StringRemoveRangeTF" in {
        val tfr = new TransformationFunctionRegistry()
        val tf = tfr.getReferenceTransformationFunction(StringRemove, ReferenceType.Range)
        tf.value shouldBe StringRemoveRangeTF
      }
    }
    
    "getting a ReferenceTransformationFunction for an StringSet and an Range reference" must {
      "return StringSetRangeTF" in {
        val tfr = new TransformationFunctionRegistry()
        val tf = tfr.getReferenceTransformationFunction(StringSet, ReferenceType.Range)
        tf.value shouldBe StringSetRangeTF
      }
    }
    

    ///////////////////////////////////////////////////////////////////////////
    // Exceptional Cases
    ///////////////////////////////////////////////////////////////////////////
    "getting a TransformationFunction for an invalid pair of operations" must {
      "return None when a StringOperation is transformed with a non StringOperation" in {
        val tfr = new TransformationFunctionRegistry()
        val tf = tfr.getOperationTransformationFunction(StringSet, NumberAdd)
        tf shouldBe None
      }

      "return None when a ArrayOperation is transformed with a non ArrayOperation" in {
        val tfr = new TransformationFunctionRegistry()
        val tf = tfr.getOperationTransformationFunction(ArraySet, NumberAdd)
        tf shouldBe None
      }

      "return None when a ObjectOperation is transformed with a non ObjectOperation" in {
        val tfr = new TransformationFunctionRegistry()
        val tf = tfr.getOperationTransformationFunction(ObjectSet, NumberAdd)
        tf shouldBe None
      }

      "return None when a NumberOperation is transformed with a non NumberOperation" in {
        val tfr = new TransformationFunctionRegistry()
        val tf = tfr.getOperationTransformationFunction(NumberSet, StringSet)
        tf shouldBe None
      }
    }
  }

  "A RegistryKey" when {
    "creating a RegistryKey using of" must {
      "create the proper instnace" in {
        RegistryKey.of[StringInsertOperation, StringRemoveOperation] shouldBe
          RegistryKey(classOf[StringInsertOperation], classOf[StringRemoveOperation])
      }
    }
  }

  "A TFMap" when {
    "registering a transfomration function" must {
      "return a registered function" in {
        val tfMap = new OTFMap()
        tfMap.register[StringInsertOperation, StringRemoveOperation](StringInsertRemoveTF)
        tfMap.getOperationTransformationFunction(StringInsert, StringRemove).value shouldBe StringInsertRemoveTF
      }

      "return None for a not registered function" in {
        val tfMap = new OTFMap()
        tfMap.getOperationTransformationFunction(StringInsert, StringRemove) shouldBe None
      }

      "disallow a duplicate registration" in {
        val tfMap = new OTFMap()
        tfMap.register[StringInsertOperation, StringRemoveOperation](StringInsertRemoveTF)
        intercept[IllegalArgumentException] {
          tfMap.register[StringInsertOperation, StringRemoveOperation](StringInsertRemoveTF)
        }
      }
    }
  }
}