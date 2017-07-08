package services

import javax.inject._
import models.{ItemList, ItemListRepository, User}
import scala.concurrent.Future

@Singleton
class ItemListService @Inject()(repo: ItemListRepository) {

  def add(name: String, description: Option[String] = None, user: User,
         uuid: Option[ItemList.UUID] = None): Future[ItemList.UUID] = {
   repo.add(name, description, user, uuid)
  }

  def updateName(name: String, uuid: ItemList.UUID): Future[Boolean] = repo.updateName(name, uuid)

  def updateDescription(description: String, uuid: ItemList.UUID): Future[Boolean] =
    repo.updateDescription(description, uuid)

  def delete(uuid: ItemList.UUID): Future[Boolean] = repo.delete(uuid)

  def get(uuid: ItemList.UUID): Future[Option[ItemList]] = repo.get(uuid)

  def listsByUser(user: User): Future[Seq[ItemList]] = repo.listsByUser(user)

}
