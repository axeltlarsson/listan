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
  dbConfigProvider.get.profile

  import profile.api._

  private[models] val items = TableQuery[Items]

  override def add(item: Item): Future[Item.UUID] = {
    db.run((items returning items.map(_.uuid)) += item)
  }

  override def delete(uuid: Item.UUID): Future[Int] = {
    val action = items.filter(_.uuid === uuid)
    db.run(action.delete)
  }

  override def edit(uuid: Item.UUID, contents: String): Future[Int] = {
    val action = for { i <- items if i.uuid === uuid } yield i.contents
    db.run(action.update(contents))
  }

  override def complete(uuid: Item.UUID): Future[Int] = {
    val selectCompleted = for { i <- items if i.uuid === uuid } yield i.completed
    db.run(for {
      maybeItem <- items.filter(_.uuid === uuid).result.headOption
      affectedRows <- selectCompleted.update(maybeItem.exists((i: Item) => true))
    } yield affectedRows)
  }

  override def unComplete(uuid: Item.UUID): Future[Int] = {
    val selectCompleted = for { i <- items if i.uuid === uuid } yield i.completed
    db.run(for {
      maybeItem <- items.filter(_.uuid === uuid).result.headOption
      affectedRows <- selectCompleted.update(maybeItem.exists((i: Item) => false))
    } yield affectedRows)
  }

  override def get(uuid: Item.UUID): Future[Option[Item]] = {
    val action = items.filter(_.uuid === uuid).result.headOption
    db.run(action)
  }

  override def itemsByList(listUuid: ItemList.UUID): Future[Seq[Item]] = {
    val action = items.filter(_.listUuid === listUuid).sortBy(_.created).result
    db.run(action)
  }

  private[models] class Items(tag: Tag) extends Table[Item](tag, "items") {
    def uuid = column[String]("uuid", O.PrimaryKey)
    def contents = column[String]("contents")
    def completed = column[Boolean]("completed")
    def listUuid = column[String]("list_uuid")
    def created = column[Timestamp]("created_at", O.AutoInc)
    def updated = column[Timestamp]("updated_at", O.AutoInc)
    def foreign_list = foreignKey("LIST_FK", listUuid, lstRepo.itemLists)(_.uuid, onUpdate=ForeignKeyAction.Restrict,
                                                                                   onDelete=ForeignKeyAction.Cascade)

    def * = (uuid, contents, completed, listUuid, created.?, updated.?) <>
      ((Item.apply _).tupled, Item.unapply)

  }

}

