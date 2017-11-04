package models

import scala.concurrent.Future

trait UserRepository {
  def authenticate(name: String, password: String): Future[Option[User]]
  def all(): Future[Seq[User]]
  def add(user: User): Future[User.UUID]
  def findByName(name: String): Future[Option[User]]
  def get(uuid: User.UUID): Future[Option[User]]
}
