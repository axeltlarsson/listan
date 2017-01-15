package services

import models.{UserRepository, User}
import javax.inject._
import scala.concurrent.Future
import play.api.Configuration
import play.Logger
import pdi.jwt.{JwtJson, JwtAlgorithm}

@Singleton
class UserService @Inject() (userRepo: UserRepository, configuration: Configuration) {
  def authenticate(name: String, password: String): Future[Option[User]] = {
    userRepo.authenticate(name, password)
  }

  def authenticate(token: String): Option[User] = {
    for {
      key <- configuration.getString("play.crypto.secret")
      decoded <- JwtJson.decodeJson(token, key, Seq(JwtAlgorithm.HmacSHA256)).toOption
      user <- (decoded \ "user").validate[User].asOpt
    } yield user
  }

}
