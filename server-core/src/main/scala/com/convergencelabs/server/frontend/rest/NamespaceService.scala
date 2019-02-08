package com.convergencelabs.server.frontend.rest

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import com.convergencelabs.server.datastore.convergence.UserStore.User
import com.convergencelabs.server.datastore.convergence.ConvergenceUserManagerActor.CreateConvergenceUserRequest
import com.convergencelabs.server.datastore.convergence.ConvergenceUserManagerActor.DeleteConvergenceUserRequest
import com.convergencelabs.server.datastore.convergence.ConvergenceUserManagerActor.GetConvergenceUser
import com.convergencelabs.server.datastore.convergence.ConvergenceUserManagerActor.GetConvergenceUsers
import com.convergencelabs.server.datastore.DuplicateValueException
import com.convergencelabs.server.datastore.InvalidValueExcpetion

import akka.actor.ActorRef
import akka.http.scaladsl.marshalling.ToResponseMarshallable.apply
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directive.addByNameNullaryApply
import akka.http.scaladsl.server.Directive.addDirectiveApply
import akka.http.scaladsl.server.Directives.Segment
import akka.http.scaladsl.server.Directives._enhanceRouteWithConcatenation
import akka.http.scaladsl.server.Directives._segmentStringToPathMatcher
import akka.http.scaladsl.server.Directives._string2NR
import akka.http.scaladsl.server.Directives.complete
import akka.http.scaladsl.server.Directives.delete
import akka.http.scaladsl.server.Directives.authorize
import akka.http.scaladsl.server.Directives.parameters
import akka.http.scaladsl.server.Directives.get
import akka.http.scaladsl.server.Directives.handleWith
import akka.http.scaladsl.server.Directives.handleExceptions
import akka.http.scaladsl.server.Directives.pathEnd
import akka.http.scaladsl.server.Directives.pathPrefix
import akka.http.scaladsl.server.Directives.put
import akka.http.scaladsl.server.Directives.post
import akka.http.scaladsl.server.Directives.delete
import akka.http.scaladsl.server.Directives.entity
import akka.http.scaladsl.server.Directives.as
import akka.util.Timeout
import akka.http.scaladsl.server.ExceptionHandler
import com.convergencelabs.server.datastore.convergence.NamespaceStoreActor.GetAccessibleNamespaces
import com.convergencelabs.server.domain.Namespace
import com.convergencelabs.server.datastore.convergence.NamespaceStoreActor.CreateNamespace
import com.convergencelabs.server.datastore.convergence.NamespaceStoreActor.DeleteNamespace
import com.convergencelabs.server.datastore.convergence.NamespaceStoreActor.UpdateNamespace
import com.convergencelabs.server.security.AuthorizationProfile
import com.convergencelabs.server.security.Permissions
import com.convergencelabs.server.domain.NamespaceAndDomains
import com.convergencelabs.server.frontend.rest.DomainService.DomainRestData

object NamespaceService {
  case class CreateNamespacePost(id: String, displayName: String)
  case class UpdateNamespacePut(displayName: String)
  case class NamespaceRestData(id: String, displayName: String)
  case class NamespaceAndDomainsRestData(id: String, displayName: String, domains: Set[DomainRestData])
}

class NamespaceService(
  private[this] val executionContext: ExecutionContext,
  private[this] val namespaceActor: ActorRef,
  private[this] val defaultTimeout: Timeout) extends JsonSupport {

  import NamespaceService._
  import akka.pattern.ask

  implicit val ec = executionContext
  implicit val t = defaultTimeout

  val route = { authProfile: AuthorizationProfile =>
    pathPrefix("namespaces") {
      pathEnd {
        get {
          complete(getNamespaces(authProfile))
        } ~ post {
          authorize(canManageNamespaces(authProfile)) {
            entity(as[CreateNamespacePost]) { request =>
              complete(createNamespace(authProfile, request))
            }
          }
        }
      } ~ pathPrefix(Segment) { namespace =>
        pathEnd {
          put {
            authorize(canManageNamespaces(authProfile)) {
              entity(as[UpdateNamespacePut]) { request =>
                complete(updateNamespace(authProfile, namespace, request))
              }
            }
          } ~ delete {
            authorize(canManageNamespaces(authProfile)) {
              complete(deleteNamespace(authProfile, namespace))
            }
          }
        }
      }
    }
  }

  def getNamespaces(authProfile: AuthorizationProfile): Future[RestResponse] = {
    val request = GetAccessibleNamespaces(authProfile)
    (namespaceActor ? request).mapTo[Set[NamespaceAndDomains]] map { namespaces =>
      val response = namespaces.map { n =>
        val domainData = n.domains.map(d => DomainRestData(d.displayName, d.domainFqn.namespace, d.domainFqn.domainId, d.status.toString))
        NamespaceAndDomainsRestData(n.id, n.displayName, domainData)
      }
      okResponse(response)
    }
  }

  def createNamespace(authProfile: AuthorizationProfile, create: CreateNamespacePost): Future[RestResponse] = {
    val CreateNamespacePost(id, displayName) = create
    val request = CreateNamespace(authProfile.username, id, displayName)
    (namespaceActor ? request).mapTo[Unit] map (_ => OkResponse)
  }

  def updateNamespace(authProfile: AuthorizationProfile, namespaceId: String, create: UpdateNamespacePut): Future[RestResponse] = {
    val UpdateNamespacePut(displayName) = create
    val request = UpdateNamespace(authProfile.username, namespaceId, displayName)
    (namespaceActor ? request).mapTo[Unit] map (_ => OkResponse)
  }

  def deleteNamespace(authProfile: AuthorizationProfile, namespaceId: String): Future[RestResponse] = {
    val request = DeleteNamespace(authProfile.username, namespaceId)
    (namespaceActor ? request).mapTo[Unit] map (_ => OkResponse)
  }

  def canManageNamespaces(authProfile: AuthorizationProfile): Boolean = {
    authProfile.hasGlobalPermission(Permissions.Global.ManageDomains)
  }
}