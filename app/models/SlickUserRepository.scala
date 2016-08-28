package models

class SlickUserRepository extends UserRepository {
  override def authenticate(userName: String, password: String): Option[User] = {
    Some(User(userName))
  }
}