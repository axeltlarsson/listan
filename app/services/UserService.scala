package services

import models.{UserRepository, User}
import javax.inject._

class UserService @Inject() (userRepo: UserRepository) {
  def authenticate(userName: String, password: String): Option[User] = {
    userRepo.authenticate(userName, password);
  }
}