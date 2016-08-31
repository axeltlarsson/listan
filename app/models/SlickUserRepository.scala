package models
import javax.inject._
import play.api.db.slick.DatabaseConfigProvider
import play.api.db.slick.HasDatabaseConfigProvider
import slick.driver.JdbcProfile

class SlickUserRepository @Inject() (protected val dbConfigProvider: DatabaseConfigProvider) extends HasDatabaseConfigProvider[JdbcProfile] with UserRepository {
  // WIP
  // import driver.api._

  // private val Users = TableQuery[UserTable]

  // def all(): Future[Seq[User]] = db.run(Users.result)

  // private class UserTable(tag: Tag) extends Table[User](tag, "USER") {
  //   def name = column[String]("NAME", O.PrimaryKey)

  //   def * = (name, color) <> (Cat.tupled, Cat.unapply _)
  // }

  override def authenticate(userName: String, password: String): Option[User] = {
    Some(User(userName))
  }
}