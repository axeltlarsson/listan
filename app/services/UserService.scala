package services

import models.{UserRepository, User}
import javax.inject._
import scala.concurrent.Future

class UserService @Inject() (userRepo: UserRepository) {
  def authenticate(userName: String, password: String): Future[Option[User]] = {
    userRepo.authenticate(userName, password);
  }
}