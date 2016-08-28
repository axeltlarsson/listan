package controllers

import javax.inject._
import play.api._
import play.api.mvc._
import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.Logger
import scala.language.postfixOps
import pdi.jwt._

import models.User
import services.UserService

@Singleton
class HomeController @Inject() (userService: UserService) extends Controller with Secured {

  def index = Action {
    Ok(views.html.index("Your new application is ready."))
  }

  private val loginData: Reads[(String, String)] =
    (JsPath \ "username").read[String] and
    (JsPath \ "password").read[String] tupled

  def login = Action(parse.json) { implicit request =>
    request.body.validate(loginData).fold(
      errors => {
        BadRequest(JsError.toJson(errors))
      },
      form => {
        userService.authenticate(form._1, form._2) match {
          case Some(user) => Ok.addingToJwtSession("user", user)
          case None => Unauthorized
        }
      }
    )
  }

  def privateApi = Authenticated { request =>
    Ok("This is some secret shit right here, be careful " + request.user)
  }

}
