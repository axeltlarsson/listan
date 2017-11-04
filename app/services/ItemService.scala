package services

import models.{Item, ItemRepository, ItemList}
import javax.inject._
import java.util.UUID.randomUUID

import scala.concurrent.Future

@Singleton
class ItemService @Inject()(repo: ItemRepository) {

  def add(contents: String, listUuid: ItemList.UUID, uuid: Option[Item.UUID] = None): Future[Item.UUID] = {
    repo.add(Item(uuid.getOrElse(randomUUID().toString), contents, false, listUuid))
  }

  def complete(uuid: Item.UUID): Future[Int] = repo.complete(uuid)

  def unComplete(uuid: Item.UUID): Future[Int] = repo.unComplete(uuid)

  def edit(uuid: Item.UUID, contents: String): Future[Int] = repo.edit(uuid, contents)

  def delete(uuid: Item.UUID): Future[Int] = repo.delete(uuid)

  def get(uuid: Item.UUID): Future[Option[Item]] = repo.get(uuid)

  def itemsByList(listUUID: ItemList.UUID): Future[Seq[Item]] = repo.itemsByList(listUUID)
}
