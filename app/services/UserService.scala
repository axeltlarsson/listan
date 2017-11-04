package services

import models.{UserRepository, User}
import javax.inject._
import scala.concurrent.Future
import play.api.Configuration
import pdi.jwt.{JwtJson, JwtAlgorithm}
import java.util.UUID.randomUUID
import com.github.t3hnar.bcrypt._

@Singleton
class UserService @Inject()(repo: UserRepository, configuration: Configuration) {

  def authenticate(name: String, password: String): Future[Option[User]] = {
    repo.authenticate(name, password)
  }

  def authenticate(token: String): Option[User] = {
    for {
      key <- configuration.getOptional[String]("play.http.secret.key")
      decoded <- JwtJson.decodeJson(token, key, Seq(JwtAlgorithm.HS256)).toOption
      user <- (decoded \ "user").validate[User].asOpt
    } yield user
  }

  def findByName(name: String): Future[Option[User]] = repo.findByName(name)

  def get(uuid: User.UUID): Future[Option[User]] = repo.get(uuid)

  def add(name: String, password: String): Future[User.UUID] = {
    repo.add(User(randomUUID().toString, name, password.bcrypt))
  }

}
