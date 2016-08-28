package models

trait UserRepository {
  def authenticate(userName: String, password: String): Option[User]
}
