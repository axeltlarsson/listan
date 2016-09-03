package models

import scala.concurrent.Future

trait ItemRepository {
  def add(contents: String): Future[Item.UUID]
  def toggle(uuid: String): Future[Item]
  def edit(uuid: String, contents: String): Future[Item]
  def delete(uuid: String): Unit
  def all(): Future[Seq[Item]]
}
