package models

import scala.concurrent.Future

trait ListRepository {
  def add(name: String, description: Option[String], user: User, uuid: Option[Lst.UUID]): Future[Lst.UUID]
  def updateName(name: String, uuid: Lst.UUID): Future[Int]
  def updateDescription(description: String, uuid: Lst.UUID): Future[Int]
  def delete(uuid: String): Future[Int]
  def all(): Future[Seq[Lst]]
}

