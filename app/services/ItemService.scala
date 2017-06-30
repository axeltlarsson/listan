package services

import models.{Item, ItemRepository, ItemList}
import javax.inject._

import scala.concurrent.Future

@Singleton
class ItemService @Inject()(itemRepo: ItemRepository) {
  def add(contents: String, lstUUID: String, uuid: Option[String] = None): Future[String] = itemRepo.add(contents, lstUUID, uuid)
  def complete(uuid: String): Future[Int] = itemRepo.complete(uuid)
  def uncomplete(uuid: String): Future[Int] = itemRepo.uncomplete(uuid)
  def edit(uuid: String, contents: String): Future[Int] = itemRepo.edit(uuid, contents)
  def delete(uuid: String): Future[Int] = itemRepo.delete(uuid)
  def all(): Future[Seq[Item]] = itemRepo.all()
}
