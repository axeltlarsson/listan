package models

import models.ItemList.UUID
import models.User.UUID

import scala.concurrent.Future

trait ItemListRepository {
  def add(uuid: UUID, user_uuid: UUID, name: String, description: Option[String]): Future[ItemList.UUID]
  def updateName(name: String, uuid: ItemList.UUID): Future[Boolean]
  def updateDescription(description: String, uuid: ItemList.UUID): Future[Boolean]
  def delete(uuid: ItemList.UUID): Future[Boolean]
  def get(uuid: ItemList.UUID): Future[Option[ItemList]]
  def listsByUser(user: User): Future[Seq[ItemList]]
}

