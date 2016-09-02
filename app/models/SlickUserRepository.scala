package models
import javax.inject._
import play.api.db.slick.DatabaseConfigProvider
import play.api.db.slick.HasDatabaseConfigProvider
import slick.driver.JdbcProfile
import scala.concurrent.{Future, Await}
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps
import java.sql.Timestamp
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Success, Failure}
import play.Logger
import com.github.t3hnar.bcrypt._

class SlickUserRepository @Inject() (protected val dbConfigProvider: DatabaseConfigProvider) extends HasDatabaseConfigProvider[JdbcProfile] with UserRepository {
  import driver.api._

  private val users = TableQuery[UsersTable]

  override def all(): Future[Seq[User]] = db.run(users.result)

  override def insert(user: User): Future[Unit] = db.run(users += user).map { _ => () }

  def find(name: String): Future[Seq[User]] = db.run(users.filter(_.name === name).result)

  private class UsersTable(tag: Tag) extends Table[User](tag, "USER") {
    def id = column[String]("UUID", O.PrimaryKey, O.AutoInc)
    def name = column[String]("NAME")
    def passwordHash = column[String]("PASSWORD_HASH")
    def created = column[Timestamp]("CREATED", O.AutoInc)
    def updated = column[Timestamp]("UPDATED", O.AutoInc)

    def idx = index("users_username_index", (name))

    def * = (name, id.?, passwordHash.?, created.?, updated.?) <> ((User.apply _).tupled, User.unapply)
  }

  override def authenticate(name: String, password: String): Future[Option[User]] = {
    Logger.info(s"authenticate($name, $password)")
    find(name) map {
      case Seq(u) if (password.isBcrypted(u.passwordHash.getOrElse(""))) => Some(u)
      case _ => None
    }
  }
}
