package models
import javax.inject._

import play.api.db.slick.DatabaseConfigProvider
import play.api.db.slick.HasDatabaseConfigProvider
import slick.jdbc.JdbcProfile

import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps
import java.sql.Timestamp

import com.github.t3hnar.bcrypt._

@Singleton
class SlickUserRepository @Inject()
    (protected val dbConfigProvider: DatabaseConfigProvider)
    (implicit ec: ExecutionContext)
    extends HasDatabaseConfigProvider[JdbcProfile] with UserRepository {

  import profile.api._

  private val users = TableQuery[UsersTable]

  override def all(): Future[Seq[User]] = db.run(users.result)

  override def insert(user: User): Future[Unit] = db.run(users += user).map { _ => () }

  def find(name: String): Future[Seq[User]] = db.run(users.filter(_.name === name).result)

  private class UsersTable(tag: Tag) extends Table[User](tag, "users") {
    def uuid = column[String]("uuid", O.PrimaryKey, O.AutoInc)
    def name = column[String]("name")
    def passwordHash = column[String]("password_hash")
    def created = column[Timestamp]("created", O.AutoInc)
    def updated = column[Timestamp]("updated", O.AutoInc)

    def idx = index("users_name_index", (name))

    def * = (name, uuid.?, passwordHash.?, created.?, updated.?) <> ((User.apply _).tupled, User.unapply)
  }

  override def authenticate(name: String, password: String): Future[Option[User]] = {
    find(name) map {
      case Seq(u) if (password.isBcrypted(u.passwordHash.getOrElse(""))) => Some(u)
      case _ => None
    }
  }
}
