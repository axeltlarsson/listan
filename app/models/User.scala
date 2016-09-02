package models

import play.api.libs.json._
import play.api.libs.functional.syntax._

case class User(userName: String, uuid: Option[String] = None, password: Option[String] = None)

object User {
  implicit val format = Json.format[User]
}
