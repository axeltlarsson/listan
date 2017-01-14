package controllers

import javax.inject._
import play.api._
import play.api.mvc._
import play.api.libs.json._
import play.api.libs.functional.syntax._
import scala.concurrent.{ExecutionContext, Future}
import play.Logger
import scala.language.postfixOps
import pdi.jwt._

import models.User
import services.UserService

@Singleton
class HomeController @Inject() (userService: UserService)(implicit exec: ExecutionContext) extends Controller with Secured {

  private val loginData: Reads[(String, String)] =
    (JsPath \ "username").read[String] and
    (JsPath \ "password").read[String] tupled

  def login = Action.async(parse.json) { implicit request =>
    request.body.validate(loginData).fold(
      errors => Future {
        BadRequest(JsError.toJson(errors))
      },
      form => {
        userService.authenticate(form._1, form._2) map {
          case Some(user) => {
            val safeUser = user.copy(
              passwordHash = None,
              uuid = None,
              created = None,
              updated = None)
            val session = JwtSession() + ("user", safeUser)
            Ok(Json.toJson(session.serialize)).addingToJwtSession("user", safeUser)
          }
          case None => Unauthorized
        }
      }
    )
  }
}

