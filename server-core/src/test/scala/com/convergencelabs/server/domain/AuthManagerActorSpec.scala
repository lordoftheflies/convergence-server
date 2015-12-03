package com.convergencelabs.server.domain

import java.util.concurrent.TimeUnit

import scala.concurrent.Await
import scala.concurrent.duration.FiniteDuration
import scala.util.Success

import org.junit.runner.RunWith
import org.mockito.Mockito
import org.scalatest.BeforeAndAfterAll
import org.scalatest.Finders
import org.scalatest.WordSpecLike
import org.scalatest.junit.JUnitRunner
import org.scalatest.mock.MockitoSugar

import com.convergencelabs.server.datastore.domain.DomainConfigStore
import com.convergencelabs.server.datastore.domain.DomainUserStore

import akka.actor.ActorSystem
import akka.testkit.TestKit

@RunWith(classOf[JUnitRunner])
class AuthenticationHandlerSpec()
    extends TestKit(ActorSystem("AuthManagerActorSpec"))
    with WordSpecLike
    with BeforeAndAfterAll
    with MockitoSugar {

  override def afterAll() {
    TestKit.shutdownActorSystem(system)
  }

  // FIXME we need to test that models actually get created and deteled.  Not sure how to do this.

  "A AuthenticationHandler" when {
    "authenticating a user by password" must {
      "authetnicate successfully for a correct username and password" in new TestFixture {
        val f = authHandler.authenticate(PasswordAuthRequest(existingUser, existingCorrectPassword))
        val result = Await.result(f, FiniteDuration(1, TimeUnit.SECONDS))
        assert(result == AuthenticationSuccess(existingUserUid, existingUser))
      }

      "Fail authetnication for an incorrect username and password" in new TestFixture {
        val f = authHandler.authenticate(PasswordAuthRequest(existingUser, existingIncorrectPassword))
        val result = Await.result(f, FiniteDuration(1, TimeUnit.SECONDS))
        assert(result == AuthenticationFailure)
      }

      "fail authenticatoin for a user that does not exist" in new TestFixture {
        val f = authHandler.authenticate(PasswordAuthRequest(nonExistingUser, ""))
        val result = Await.result(f, FiniteDuration(1, TimeUnit.SECONDS))
        assert(result == AuthenticationFailure)
      }
    }
  }

  trait TestFixture {
    val existingUser = "existing"
    val existingUserUid = "uid"
    val existingCorrectPassword = "correct"
    val existingIncorrectPassword = "incorrect"

    val nonExistingUser = "non-existing"

    val domainFqn = DomainFqn("convergence", "default")

    val userStore = mock[DomainUserStore]
    Mockito.when(userStore.domainUserExists(existingUser)).thenReturn(Success(true))
    Mockito.when(userStore.domainUserExists(nonExistingUser)).thenReturn(Success(false))
    Mockito.when(userStore.validateCredentials(existingUser, existingCorrectPassword)).thenReturn(Success(true, Some(existingUserUid)))
    Mockito.when(userStore.validateCredentials(existingUser, existingIncorrectPassword)).thenReturn(Success(false, None))
    Mockito.when(userStore.validateCredentials(nonExistingUser, "")).thenReturn(Success(false, None))

    val domainConfigStore = mock[DomainConfigStore]

    val authHandler = new AuthenticationHandler(domainConfigStore, userStore, system.dispatcher)
  }
}