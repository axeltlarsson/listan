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
                                        extends HasDatabaseConfigProvider[JdbcProfile] with LstRepository {
  import profile.api._

  private[models] val lsts = TableQuery[Lsts]

  override def add(name: String, description: Option[String], user: User,
                   uuid: Option[ItemList.UUID] = None): Future[ItemList.UUID] = {
    val itemList: ItemList = ItemList(name = name,
                       description = description,
                       userUuid = user.uuid.get,
                       uuid = uuid)
    println(s"inserting $itemList")
    if (uuid.isDefined) {
      println("uuid defined")
      db.run(DBIO.seq(lsts forceInsert itemList)).map {_ => uuid.get}
    } else {
      println("uuid not defined")
      db.run((lsts returning lsts.map(_.uuid)) += itemList)
    }
  }

  override def updateName(name: String, uuid: ItemList.UUID): Future[Boolean] = {
    val action = for { l <- lsts if l.uuid === uuid } yield l.name
    db.run(action.update(name)).map(_ == 1)
  }

  override def updateDescription(description: String, uuid: ItemList.UUID): Future[Boolean] = {
    val action = for { l <- lsts if l.uuid === uuid } yield l.description
    db.run(action.update(description)).map(_ == 1)
  }


  override def delete(uuid: ItemList.UUID): Future[Boolean] = {
    val action = lsts.filter(_.uuid === uuid)
    db.run(action.delete).map(_ == 1)
  }


  override def all(): Future[Seq[ItemList]] = db.run(lsts.sortBy(_.created).result)

  private[models] class Lsts(tag: Tag) extends Table[ItemList](tag, "lists") {
    def uuid = column[String]("uuid", O.PrimaryKey, O.AutoInc)
    def name = column[String]("name")
    def description = column[String]("description")
    def user_uuid = column[String]("user_uuid")
    def created = column[Timestamp]("created", O.AutoInc)
    def updated = column[Timestamp]("updated", O.AutoInc)
    def foreign_user = foreignKey("user_uuid", user_uuid, userRepo.users)(_.uuid)

    def * = (uuid.?, name, description.?, user_uuid, created.?, updated.?) <>
      ((ItemList.apply _).tupled, ItemList.unapply)

  }

}

