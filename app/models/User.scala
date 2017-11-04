package models

import play.api.libs.json._
import play.api.libs.functional.syntax._
import java.sql.Timestamp

import julienrf.json.derived
import julienrf.json.derived.NameAdapter
import models.User.UUID

case class User(uuid: UUID, name: String, passwordHash: String, created: Option[Timestamp] = None, updated: Option[Timestamp] = None)

object User {
  type UUID = String
  implicit val rds: Reads[Timestamp] = (__ \ "time").read[Long].map{ long => new Timestamp(long) }
  implicit val wrs: Writes[Timestamp] = (__ \ "time").write[Long].contramap{ (a: Timestamp) => a.getTime }
  implicit val fmt: Format[Timestamp] = Format(rds, wrs)
  implicit val format: OFormat[User] = derived.oformat(NameAdapter.snakeCase)
}
