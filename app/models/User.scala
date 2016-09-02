package models

import play.api.libs.json._
import play.api.libs.functional.syntax._
import java.sql.Timestamp
import com.github.t3hnar.bcrypt._

case class User(
  name: String,
  uuid: Option[String] = None,
  passwordHash: Option[String] = None,
  created: Option[Timestamp] = None,
  updated: Option[Timestamp] = None)

object User {
  implicit val rds: Reads[Timestamp] = (__ \ "time").read[Long].map{ long => new Timestamp(long) }
  implicit val wrs: Writes[Timestamp] = (__ \ "time").write[Long].contramap{ (a: Timestamp) => a.getTime }
  implicit val fmt: Format[Timestamp] = Format(rds, wrs)
  implicit val format = Json.format[User]

  def create(name: String, password: String) = {
    new User(name, passwordHash = Some(password.bcrypt))
  }
}
