package models
import javax.inject._
import play.api.db.slick.DatabaseConfigProvider
import play.api.db.slick.HasDatabaseConfigProvider
import play.api.db.slick._
import slick.driver.JdbcProfile
import scala.concurrent.{Future, Await}
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps
import java.sql.Timestamp
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Success, Failure}
import play.Logger


@Singleton
class SlickItemRepository @Inject()
    (protected val dbConfigProvider: DatabaseConfigProvider)
    extends HasDatabaseConfigProvider[JdbcProfile] with ItemRepository {

  import driver.api._

  private val items = TableQuery[Items]

  override def add(uuid: String, contents: String): Future[Item.UUID] = {
    val item: Item = Item(contents, false, uuid)
    val id = (items returning items.map(_.uuid)) += item
    db.run(id)
  }

  override def delete(uuid: Item.UUID): Future[Int] = {
    val action = items.filter(_.uuid === uuid)
    db.run(action.delete)
  }

  override def edit(uuid: String, contents: String): Future[Int] = {
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

  override def uncomplete(uuid: Item.UUID): Future[Int] = {
    val selectCompleted = for { i <- items if i.uuid === uuid } yield i.completed
    db.run(for {
      maybeItem <- items.filter(_.uuid === uuid).result.headOption
      affectedRows <- selectCompleted.update(maybeItem.exists((i: Item) => false))
    } yield affectedRows)
  }

  override def all(): Future[Seq[Item]] = db.run(items.result)

  private class Items(tag: Tag) extends Table[Item](tag, "items") {
    def uuid = column[String]("uuid", O.PrimaryKey)
    def contents = column[String]("contents")
    def completed = column[Boolean]("completed")
    def created = column[Timestamp]("created", O.AutoInc)
    def updated = column[Timestamp]("updated", O.AutoInc)

    def * = (contents, completed, uuid, created.?, updated.?) <>
      ((Item.apply _).tupled, Item.unapply)

  }

}

