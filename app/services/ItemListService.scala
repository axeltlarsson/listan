package services

import javax.inject._

import models._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ItemListService @Inject()(repo: ItemListRepository, itemRepo: ItemRepository)(implicit ec: ExecutionContext) {

  def add(name: String, description: Option[String] = None, user_uuid: User.UUID,
         uuid: Option[ItemList.UUID] = None): Future[ItemList.UUID] = {
   repo.add(name, description, user_uuid, uuid)
  }

  def updateName(name: String, uuid: ItemList.UUID): Future[Boolean] = repo.updateName(name, uuid)

  def updateDescription(description: String, uuid: ItemList.UUID): Future[Boolean] =
    repo.updateDescription(description, uuid)

  def delete(uuid: ItemList.UUID): Future[Boolean] = repo.delete(uuid)

  def get(uuid: ItemList.UUID): Future[Option[ItemList]] = repo.get(uuid)

  def listsByUser(user: User): Future[Seq[ItemList]] = repo.listsByUser(user)

  def itemListsByUser(user: User): Future[Seq[(ItemList, Seq[Item])]] = {
    for {
      lists <- listsByUser(user)
      // Doing just lists.map(itemRepo.itemsByList(_.uuid)) would become Seq[Future[Seq[Item]]
      // traverse fixes this into Future[Seq[Item]], then the .map on the future transforms the Seq[Item] into a tuple
      // of (ItemList, Seq[Item]) which is what we want
      items <- Future.traverse(lists)(list => itemRepo.itemsByList(list.uuid.get).map(items => (list, items)))
    } yield items
  }

}
