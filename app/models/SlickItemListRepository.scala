package models

import java.sql.Timestamp
import javax.inject._

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

  override def add(itemList: ItemList): Future[ItemList.UUID] = {
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
    val action = itemLists.filter(_.userUuid === user.uuid).sortBy(_.created).result
    db.run(action)
  }

  private[models] class ItemLists(tag: Tag) extends Table[ItemList](tag, "lists") {
    def uuid = column[String]("uuid", O.PrimaryKey)
    def name = column[String]("name", O.Unique)
    def description = column[String]("description")
    def userUuid = column[String]("user_uuid")
    def created = column[Timestamp]("created_at", O.AutoInc)
    def updated = column[Timestamp]("updated_at", O.AutoInc)
    def foreign_user = foreignKey("user_uuid", userUuid, userRepo.users)(_.uuid, onUpdate=ForeignKeyAction.Restrict)

    def * = (uuid, name, description.?, userUuid, created.?, updated.?) <>
      ((ItemList.apply _).tupled, ItemList.unapply)

  }

}

