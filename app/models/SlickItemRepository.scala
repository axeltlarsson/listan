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

  override def add(contents: String): Future[Item.UUID] = {
    val item = Item(contents)
    val uuid = (items returning items.map(_.uuid)) += item
    db.run(uuid)
  }

  override def delete(uuid: Item.UUID): Future[Int] = {
    val action = items.filter(_.uuid === uuid)
    db.run(action.delete)
  }

  override def edit(uuid: String, contents: String): Future[Int] = {
    val action = for { i <- items if i.uuid === uuid } yield i.contents
    db.run(action.update(contents))
  }

  override def toggle(uuid: String): Future[Int] = {
    val selectCompleted = for { i <- items if i.uuid === uuid } yield i.completed
    db.run(for {
      item <- items.filter(_.uuid === uuid).result.head
      affectedRows <- selectCompleted.update(!item.completed)
    } yield affectedRows)
  }

  override def all(): Future[Seq[Item]] = db.run(items.result)

  private class Items(tag: Tag) extends Table[Item](tag, "items") {
    def uuid = column[String]("uuid", O.PrimaryKey, O.AutoInc)
    def contents = column[String]("contents")
    def completed = column[Boolean]("completed")
    def created = column[Timestamp]("created", O.AutoInc)
    def updated = column[Timestamp]("updated", O.AutoInc)

    def * = (contents, completed, uuid.?, created.?, updated.?) <>
      ((Item.apply _).tupled, Item.unapply)

  }

}
