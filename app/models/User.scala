package models

import play.api.libs.json._
import play.api.libs.functional.syntax._

case class User(name: String)

object User {
  implicit val format = Json.format[User]
}
