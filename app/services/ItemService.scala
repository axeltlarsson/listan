package services

import models.{ItemRepository, Item}
import javax.inject._
import scala.concurrent.Future
import play.Logger

@Singleton
class ItemService @Inject()(itemRepo: ItemRepository) {
  def add(contents: String): Future[Item.UUID] = itemRepo.add(contents)
  def complete(uuid: String): Future[Int] = itemRepo.complete(uuid)
  def uncomplete(uuid: String): Future[Int] = itemRepo.uncomplete(uuid)
  def edit(uuid: String, contents: String): Future[Int] = itemRepo.edit(uuid, contents)
  def delete(uuid: String): Future[Int] = itemRepo.delete(uuid)
  def all(): Future[Seq[Item]] = itemRepo.all()
}
