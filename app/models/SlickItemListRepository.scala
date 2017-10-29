package models

import java.sql.Timestamp
import javax.inject._

import models.ItemList.UUID
import models.User.UUID
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SlickItemListRepository @Inject()(protected val dbConfigProvider: DatabaseConfigProvider,
                                        private[models] val userRepo: SlickUserRepository)
                                       (implicit ec: ExecutionContext)
                                        extends HasDatabaseConfigProvider[JdbcProfile] with ItemListRepository {
  import profile.api._

  private[models] val itemLists = TableQuery[ItemLists]

  // TODO: do not allow uuid to empty
  override def add(uuid: UUID, user_uuid: UUID, name: String, description: Option[String]): Future[ItemList.UUID] = {
    val itemList: ItemList = ItemList(name = name,
                       description = description,
                       userUuid = user_uuid,
                       uuid = Some(uuid))
    db.run((itemLists returning itemLists.map(_.uuid)) += itemList)
  }

  override def updateName(name: String, uuid: ItemList.UUID): Future[Boolean] = {
    val query = for {l <- itemLists if l.uuid === uuid } yield l.name
    db.run(query.update(name)).map(_ == 1)
  }

  override def updateDescription(description: String, uuid: ItemList.UUID): Future[Boolean] = {
    val query = for {l <- itemLists if l.uuid === uuid } yield l.description
    db.run(query.update(description)).map(_ == 1)
  }

  override def delete(uuid: ItemList.UUID): Future[Boolean] = {
    // `ON DELETE CASCADE` on the foreign key to lists in items ensures items are deleted first
    val query = itemLists.filter(_.uuid === uuid)
    val action = query.delete.map(_ == 1)
    db.run(action)
  }

  override def get(uuid: ItemList.UUID): Future[Option[ItemList]] = {
    val action = itemLists.filter(_.uuid === uuid).result.headOption
    db.run(action)
  }

  override def listsByUser(user: User): Future[Seq[ItemList]] = {
    val action = itemLists.filter(_.user_uuid === user.uuid).sortBy(_.created).result
    db.run(action)
  }

  private[models] class ItemLists(tag: Tag) extends Table[ItemList](tag, "lists") {
    def uuid = column[String]("uuid", O.PrimaryKey, O.AutoInc)
    def name = column[String]("name", O.Unique)
    def description = column[String]("description")
    def user_uuid = column[String]("user_uuid")
    def created = column[Timestamp]("created", O.AutoInc)
    def updated = column[Timestamp]("updated", O.AutoInc)
    def foreign_user = foreignKey("user_uuid", user_uuid, userRepo.users)(_.uuid, onUpdate=ForeignKeyAction.Restrict)

    def * = (uuid.?, name, description.?, user_uuid, created.?, updated.?) <>
      ((ItemList.apply _).tupled, ItemList.unapply)

  }

}

