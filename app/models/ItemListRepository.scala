package models

import models.ItemList.UUID
import models.User.UUID

import scala.concurrent.Future

trait ItemListRepository {
  def add(itemList: ItemList): Future[ItemList.UUID]
  def updateName(name: String, uuid: ItemList.UUID): Future[Boolean]
  def updateDescription(description: String, uuid: ItemList.UUID): Future[Boolean]
  def delete(uuid: ItemList.UUID): Future[Boolean]
  def get(uuid: ItemList.UUID): Future[Option[ItemList]]
  def listsByUser(user: User): Future[Seq[ItemList]]
}

