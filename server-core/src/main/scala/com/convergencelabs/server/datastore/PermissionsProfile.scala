package com.convergencelabs.server.datastore

class PermissionsProfile(private[this] val roles: Set[Role]) {
  
  private[this] val permissions: Set[String] = roles.map { _.permissions }.flatten
  
  def hasRole(role: String): Boolean = {
    roles.exists { _.name == role }
  }
  
  def hasPermission(permission: String): Boolean = {
    permission.contains(permission)
  }
}