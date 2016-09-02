package models

import scala.concurrent.Future

trait UserRepository {
  def authenticate(userName: String, password: String): Future[Option[User]]
  def all(): Future[Seq[User]]
}
