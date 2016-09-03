package models

import scala.concurrent.Future

trait ItemRepository {
  def add(contents: String): Future[Item.UUID]
  def toggle(uuid: String): Future[Int]
  def edit(uuid: String, contents: String): Future[Int]
  def delete(uuid: String): Future[Int]
  def all(): Future[Seq[Item]]
}
