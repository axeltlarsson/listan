package services

import models.{UserRepository, User}
import javax.inject._
import scala.concurrent.Future
import play.api.libs.json.{Json, JsValue, JsError, JsSuccess}
import play.api.Configuration
import play.Logger
import scala.util.Success

class UserService @Inject() (userRepo: UserRepository, configuration: Configuration) {
  def authenticate(name: String, password: String): Future[Option[User]] = {
    userRepo.authenticate(name, password)
  }
 
  def authenticate(token: String): Option[User] = {
    import pdi.jwt.{JwtJson, JwtAlgorithm}
    val key = configuration.getString("play.crypto.secret").get
    JwtJson.decodeJson(token, key, Seq(JwtAlgorithm.HmacSHA256)) match {
      case Success(json) => {
        Logger.info("token decoded as " + json)
        (json \ "user").validate[User] match {
          case JsSuccess(user, _) => {
            Logger.info("Authenticated " + user)
            Some(user)
          }
          case _ => None
        }
      }
      case _ => {
        Logger.error("could not decode token")
        None
      }
    }
  }

}
