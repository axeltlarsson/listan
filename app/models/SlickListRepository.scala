package models

import javax.inject.Inject

import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile

import scala.concurrent.{ExecutionContext, Future}


@Singleton
class SlickListRepository @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)
                                   (implicit ec: ExecutionContext)
                                    extends HasDatabaseConfigProvider[JdbcProfile] with ListRepository {
  import profile.api._

  val listsTable = TableQuery[ListsTable]

  override def add(name: String, description: Option[String], user: User,
                   uuid: Option[Lst.UUID] = None): Future[Lst.UUID] = {
    val list: Lst = Lst(name = name, description = description, userUuid = user.uuid.get, uuid = uuid)
    if (uuid.isDefined) {
      db.run(DBIO.seq(listsTable forceInsert list)).map {_ => id.get}
    } else {
      db.run((listsTable returning listsTable.map(_.uuid)) += item)
    }
  }

  override def delete(uuid: Item.UUID): Future[Int] = {
    val action = items.filter(_.uuid === uuid)
    db.run(action.delete)
  }

  override def edit(uuid: String, contents: String): Future[Int] = {
    val action = for { i <- items if i.uuid === uuid } yield i.contents
    db.run(action.update(contents))
  }

  override def all(): Future[Seq[Item]] = db.run(items.sortBy(_.created).result)

  private class ListsTable(tag: Tag) extends Table[Lst](tag, "lists") {
    def uuid = column[String]("uuid", O.PrimaryKey, O.AutoInc)
    def name = column[String]("name")
    def user_uuid = column[String]("user_uuid", O.AccutoInc)
    def created = column[Timestamp]("created", O.AutoInc)
    def updated = column[Timestamp]("updated", O.AutoInc)
    def foreign_user = foreignKey("USER_FK", "user_uuid", models.SlickUserRepository.Users)(_.uuid)

    def * = (contents, completed, uuid.?, created.?, updated.?) <>
      ((Item.apply _).tupled, Item.unapply)

  }

}

