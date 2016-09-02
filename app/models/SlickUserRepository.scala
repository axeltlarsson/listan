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

class SlickUserRepository @Inject() (protected val dbConfigProvider: DatabaseConfigProvider) extends HasDatabaseConfigProvider[JdbcProfile] with UserRepository {
  import driver.api._

  private val Users = TableQuery[UsersTable]

  def all(): Future[Seq[User]] = db.run(Users.result)

  private class UsersTable(tag: Tag) extends Table[User](tag, "USER") {
    def uuid = column[String]("UUID", O.PrimaryKey)
    def userName = column[String]("USER_NAME")
    def password = column[String]("PASSWORD")
    def created_at = column[Timestamp]("CREATED_AT")
    def updated_at = column[Timestamp]("UPDATED_AT")

    def idx = index("users_username_index", (userName))

    def * = (userName, uuid.?, password.?) <> ((User.apply _).tupled, User.unapply)
  }

  override def authenticate(userName: String, password: String): Future[Option[User]] = {
    Logger.info(s"authenticate($userName, $password)")
    val users =  Await.result(all(), 1 seconds)
    users.foreach(user => Logger.info(user.userName))
    Future {Some(User(userName))}
  }
}