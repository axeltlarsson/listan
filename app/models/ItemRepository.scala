package models

import scala.concurrent.Future

trait ItemRepository {
  def add(item: Item): Future[Item.UUID]
  def complete(uuid: Item.UUID): Future[Int]
  def unComplete(uuid: Item.UUID): Future[Int]
  def edit(uuid: Item.UUID, contents: String): Future[Int]
  def delete(uuid: Item.UUID): Future[Int]
  def get(uuid: Item.UUID): Future[Option[Item]]
  def itemsByList(listUuid: ItemList.UUID): Future[Seq[Item]]
}
