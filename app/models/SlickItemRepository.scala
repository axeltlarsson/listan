package models
import javax.inject._

import play.api.db.slick.DatabaseConfigProvider
import play.api.db.slick.HasDatabaseConfigProvider
import slick.jdbc.JdbcProfile

import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps
import java.sql.Timestamp

@Singleton
class SlickItemRepository @Inject()
    (protected val dbConfigProvider: DatabaseConfigProvider, private[models] val lstRepo: SlickItemListRepository)
    (implicit ec: ExecutionContext)
    extends HasDatabaseConfigProvider[JdbcProfile] with ItemRepository {

  import profile.api._

  private[models] val items = TableQuery[Items]

  override def add(contents: String, listUUID: String, uuid: Option[String] = None): Future[String] = {
    val item: Item = Item(uuid = uuid, contents = contents, listUuid = listUUID)
    if (uuid.isDefined) {
      db.run(DBIO.seq(items forceInsert item)).map {_ => uuid.get}
    } else {
      db.run((items returning items.map(_.uuid)) += item)
    }
  }

  override def delete(uuid: String): Future[Int] = {
    val action = items.filter(_.uuid === uuid)
    db.run(action.delete)
  }

  override def edit(uuid: String, contents: String): Future[Int] = {
    val action = for { i <- items if i.uuid === uuid } yield i.contents
    db.run(action.update(contents))
  }

  override def complete(uuid: String): Future[Int] = {
    val selectCompleted = for { i <- items if i.uuid === uuid } yield i.completed
    db.run(for {
      maybeItem <- items.filter(_.uuid === uuid).result.headOption
      affectedRows <- selectCompleted.update(maybeItem.exists((i: Item) => true))
    } yield affectedRows)
  }

  override def unComplete(uuid: String): Future[Int] = {
    val selectCompleted = for { i <- items if i.uuid === uuid } yield i.completed
    db.run(for {
      maybeItem <- items.filter(_.uuid === uuid).result.headOption
      affectedRows <- selectCompleted.update(maybeItem.exists((i: Item) => false))
    } yield affectedRows)
  }

  override def all(): Future[Seq[Item]] = db.run(items.sortBy(_.created).result)

  private[models] class Items(tag: Tag) extends Table[Item](tag, "items") {
    def uuid = column[String]("uuid", O.PrimaryKey, O.AutoInc)
    def contents = column[String]("contents")
    def completed = column[Boolean]("completed")
    def list_uuid = column[String]("list_uuid")
    def created = column[Timestamp]("created", O.AutoInc)
    def updated = column[Timestamp]("updated", O.AutoInc)
    def foreign_list = foreignKey("LIST_FK", list_uuid, lstRepo.itemLists)(_.uuid)

    def * = (uuid.?, contents, completed, list_uuid, created.?, updated.?) <>
      ((Item.apply _).tupled, Item.unapply)

  }

}

