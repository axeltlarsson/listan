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

  override def delete(uuid: String): Unit = ???
  override def edit(uuid: String, contents: String): Future[Item] = ???
  override def toggle(uuid: String): Future[Item] = ???
  override def all(): Future[Seq[Item]] = db.run(items.result)

  // override def insert(user: User): Future[Unit] = db.run(users += user).map { _ => () }

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
