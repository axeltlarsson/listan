package models
import javax.inject._

import play.api.db.slick.DatabaseConfigProvider
import play.api.db.slick.HasDatabaseConfigProvider
import slick.jdbc.JdbcProfile

import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps
import java.sql.Timestamp

import com.github.t3hnar.bcrypt._
import models.User.UUID

@Singleton
class SlickUserRepository @Inject()
    (protected val dbConfigProvider: DatabaseConfigProvider)
    (implicit ec: ExecutionContext)
    extends HasDatabaseConfigProvider[JdbcProfile] with UserRepository {

  import profile.api._

  private[models] val users = TableQuery[Users]

  override def all(): Future[Seq[User]] = {
    db.run(users.result)
  }

  override def add(user: User): Future[User.UUID] = {
    db.run((users returning users.map(_.uuid)) += user)
  }

  override def findByName(name: String): Future[Option[User]] ={
    db.run(users.filter(_.name === name).result.headOption)
  }

  override def get(uuid: UUID): Future[Option[User]] = {
    db.run(users.filter(_.uuid === uuid).result.headOption)
  }

  override def authenticate(name: String, password: String): Future[Option[User]] = {
    findByName(name) map {
      case Some(u) if password.isBcrypted(u.passwordHash) => Some(u)
      case _ => None
    }
  }

  private[models] class Users(tag: Tag) extends Table[User](tag, "users") {
    def uuid = column[String]("uuid", O.PrimaryKey)
    def name = column[String]("name", O.Unique)
    def passwordHash = column[String]("password_hash")
    def created = column[Timestamp]("created_at", O.AutoInc)
    def updated = column[Timestamp]("updated_at", O.AutoInc)

    def idx = index("users_name_index", (name))

    def * = (uuid, name, passwordHash, created.?, updated.?) <> ((User.apply _).tupled, User.unapply)
  }
}
