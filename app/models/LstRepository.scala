package models

import scala.concurrent.Future

trait LstRepository {
  def add(name: String, description: Option[String], user: User, uuid: Option[ItemList.UUID]): Future[ItemList.UUID]
  def updateName(name: String, uuid: ItemList.UUID): Future[Boolean]
  def updateDescription(description: String, uuid: ItemList.UUID): Future[Boolean]
  def delete(uuid: ItemList.UUID): Future[Boolean]
  def all(): Future[Seq[ItemList]]
}

