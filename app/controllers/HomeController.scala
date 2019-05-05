package controllers

import javax.inject._

import pdi.jwt._
import play.api._
import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.api.mvc._
import services.UserService

import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps

@Singleton
class HomeController @Inject()(cc: ControllerComponents, userService: UserService)
                              (implicit ec: ExecutionContext, conf: Configuration)
                              extends AbstractController(cc) {

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
            val safeUser = user.copy(passwordHash = "", created = None, updated = None)
            val session = JwtSession() + ("user", safeUser)
            Ok(Json.obj("token" -> session.serialize))
          }
          case None => Unauthorized
        }
      }
    )
  }
}

