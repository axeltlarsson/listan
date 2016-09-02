package services

import models.{UserRepository, User}
import javax.inject._
import scala.concurrent.Future

class UserService @Inject() (userRepo: UserRepository) {
  def authenticate(name: String, password: String): Future[Option[User]] = {
    userRepo.authenticate(name, password)
  }
}
