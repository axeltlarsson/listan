package models

import scala.concurrent.Future

trait UserRepository {
  def authenticate(name: String, password: String): Future[Option[User]]
  def all(): Future[Seq[User]]
  def insert(user: User): Future[Unit]
}