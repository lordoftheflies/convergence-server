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

package com.convergencelabs.convergence.server.datastore.convergence

import org.scalatest.Matchers
import org.scalatest.OptionValues.convertOptionToValuable
import org.scalatest.TryValues.convertTryToSuccessOrFailure
import org.scalatest.WordSpecLike

import com.convergencelabs.convergence.server.datastore.convergence.UserStore.User
import com.convergencelabs.convergence.server.datastore.domain.PersistenceStoreSpec
import com.convergencelabs.convergence.server.db.schema.DeltaCategory
import com.convergencelabs.convergence.server.domain.Domain
import com.convergencelabs.convergence.server.domain.DomainId
import com.convergencelabs.convergence.server.domain.DomainStatus
import com.convergencelabs.convergence.server.db.DatabaseProvider
import com.convergencelabs.convergence.server.datastore.DuplicateValueException
import com.convergencelabs.convergence.server.datastore.EntityNotFoundException
import com.convergencelabs.convergence.server.domain.DomainDatabase
import com.convergencelabs.convergence.server.domain.Namespace

object DomainStoreSpec {
  case class SpecStores(namespace: NamespaceStore, domain: DomainStore)
}

class DomainStoreSpec
  extends PersistenceStoreSpec[DomainStoreSpec.SpecStores](DeltaCategory.Convergence)
  with WordSpecLike
  with Matchers {

  def createStore(dbProvider: DatabaseProvider): DomainStoreSpec.SpecStores = {
    DomainStoreSpec.SpecStores(new NamespaceStore(dbProvider), new DomainStore(dbProvider))
  }

  val namespace1 = "namespace1"
  val namespace2 = "namespace2"

  val domain1 = "domain1"
  val domain2 = "domain2"
  val domain3 = "domain3"

  val Namespace1 = Namespace(namespace1, "Namespace 1", false)
  val Namespace2 = Namespace(namespace2, "Namespace 2", false)

  val ns1d1 = DomainId(namespace1, domain1)
  val ns1d1Database = DomainDatabase("ns1d1", "username", "password", "adminUsername", "adminPassword")

  val ns1d2 = DomainId(namespace1, domain2)
  val ns1d2Database = DomainDatabase("ns1d2", "username", "password", "adminUsername", "adminPassword")

  val ns1d3 = DomainId(namespace1, domain3)
  val ns1d3Database = DomainDatabase("ns1d3", "username", "password", "adminUsername", "adminPassword")

  val ns2d1 = DomainId("namespace2", "domain1")
  val ns2d1Database = DomainDatabase("ns2d1", "username", "password", "adminUsername", "adminPassword")

  val Username = "test"
  val NotOwner = User("other", "otherEmail", "first", "last", "display", None)
  val Password = "password"
  val Token = "token"

  "A DomainStore" when {

    "asked whether a domain exists" must {
      "return false if it doesn't exist" in withTestData { stores =>
        stores.domain.domainExists(DomainId("notRealNs", "notRealId")).get shouldBe false
      }

      "return true if it does exist" in withTestData { stores =>
        stores.domain.createDomain(ns1d1, "", ns1d1Database).get
        stores.domain.domainExists(ns1d1).get shouldBe true
      }
    }

    "retrieving a domain by fqn" must {
      "return None if the domain doesn't exist" in withTestData { stores =>
        stores.domain.createDomain(ns1d1, "", ns1d1Database).get
        stores.domain.getDomainByFqn(DomainId("notReal", "notReal")).get shouldBe None
      }

      "return Some if the domain exist" in withTestData { stores =>
        stores.domain.createDomain(ns1d1, "", ns1d1Database).get
        stores.domain.getDomainByFqn(ns1d1).success.get shouldBe defined
      }
    }

    "creating a domain" must {
      "insert the domain correct record into the database" in withTestData { stores =>
        val dbName = "t4"
        val fqn = DomainId(Namespace1.id, "test4")
        val domain = Domain(fqn, "Test Domain 4", DomainStatus.Initializing, "")
        stores.domain.createDomain(fqn, "Test Domain 4", DomainDatabase("db", "", "", "", "")).get
        stores.domain.getDomainByFqn(fqn).get.value shouldBe domain
      }

      "return a DuplicateValueExcpetion if the domain exists" in withTestData { stores =>
        stores.domain.createDomain(ns1d1, "", ns1d1Database)
        stores.domain.createDomain(ns1d1, "Test Domain 1", ns1d1Database).failure.exception shouldBe a[DuplicateValueException]
      }
    }

    "getting domains by namespace" must {
      "return all domains for a namespace" in withTestData { stores =>
        stores.domain.createDomain(ns1d1, "", ns1d1Database).get
        stores.domain.createDomain(ns1d2, "", ns1d2Database).get
        stores.domain.createDomain(ns2d1, "", ns2d1Database).get

        val domains = stores.domain.getDomainsInNamespace(namespace1).get
        domains.map { x => x.domainFqn }.toSet shouldBe Set(ns1d1, ns1d2)
      }
    }

    "removing a domain" must {
      "remove the domain record in the database if it exists" in withTestData { stores =>
        stores.domain.createDomain(ns1d1, "", ns1d1Database).get
        stores.domain.createDomain(ns1d2, "", ns1d2Database).get
        stores.domain.removeDomain(ns1d1).get
        stores.domain.domainExists(ns1d1).get shouldBe false
        stores.domain.domainExists(ns1d2).get shouldBe true
      }

      "not throw an exception if the domain does not exist" in withTestData { stores =>
        stores.domain.removeDomain(ns1d3).failure.exception shouldBe a[EntityNotFoundException]
      }
    }

    "updating a domain" must {
      "sucessfully update an existing domain" in withTestData { stores =>
        stores.domain.createDomain(ns1d1, "", ns1d1Database).get
        val toUpdate = Domain(DomainId(namespace1, domain1), "Updated", DomainStatus.Offline, "offline")
        stores.domain.updateDomain(toUpdate).get
        val queried = stores.domain.getDomainByFqn(ns1d1).get.value
        queried shouldBe toUpdate
      }

      "fail to update an non-existing domain" in withTestData { stores =>
        val toUpdate = Domain(DomainId(namespace1, domain3), "Updated", DomainStatus.Online, "")
        stores.domain.updateDomain(toUpdate).failure.exception shouldBe a[EntityNotFoundException]
      }
    }
  }

  def withTestData(testCode: DomainStoreSpec.SpecStores => Any): Unit = {
    this.withPersistenceStore { stores =>
      stores.namespace.createNamespace(Namespace1).get
      stores.namespace.createNamespace(Namespace2).get
      testCode(stores)
    }
  }
}
