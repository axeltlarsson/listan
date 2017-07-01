package services

import models.{Item, ItemRepository, ItemList}
import javax.inject._

import scala.concurrent.Future

@Singleton
class ItemService @Inject()(itemRepo: ItemRepository) {

  def add(contents: String, listUuid: ItemList.UUID, uuid: Option[Item.UUID] = None): Future[String] =
    itemRepo.add(contents, listUuid, uuid)

  def complete(uuid: String): Future[Int] = itemRepo.complete(uuid)

  def unComplete(uuid: Item.UUID): Future[Int] = itemRepo.unComplete(uuid)

  def edit(uuid: Item.UUID, contents: String): Future[Int] = itemRepo.edit(uuid, contents)

  def delete(uuid: Item.UUID): Future[Int] = itemRepo.delete(uuid)

  def all(): Future[Seq[Item]] = itemRepo.all()
}
