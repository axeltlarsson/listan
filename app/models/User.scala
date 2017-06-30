package models

import play.api.libs.json._
import play.api.libs.functional.syntax._
import java.sql.Timestamp

import com.github.t3hnar.bcrypt._

case class User(
 uuid: Option[User.UUID] = None,
 name: String,
 passwordHash: Option[String] = None,
 created: Option[Timestamp] = None,
 updated: Option[Timestamp] = None)

object User {
  type UUID = String
  implicit val rds: Reads[Timestamp] = (__ \ "time").read[Long].map{ long => new Timestamp(long) }
  implicit val wrs: Writes[Timestamp] = (__ \ "time").write[Long].contramap{ (a: Timestamp) => a.getTime }
  implicit val fmt: Format[Timestamp] = Format(rds, wrs)
  implicit val format = Json.format[User]

  /**
    * Create a new User with a hashed password
    */
  def create(name: String, password: String): User = {
    new User(name = name, passwordHash = Some(password.bcrypt))
  }
}
