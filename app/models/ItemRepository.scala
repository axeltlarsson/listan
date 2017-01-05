package models

import scala.concurrent.Future

trait ItemRepository {
  def add(uuid: String, contents: String): Future[Item.UUID]
  def complete(uuid: String): Future[Int]
  def uncomplete(uuid: String): Future[Int]
  def edit(uuid: String, contents: String): Future[Int]
  def delete(uuid: String): Future[Int]
  def all(): Future[Seq[Item]]
}
