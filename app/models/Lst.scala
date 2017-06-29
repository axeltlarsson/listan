package models

import java.sql.Timestamp
import play.api.libs.json.{Json, OFormat, Reads, Writes}

object Lst {
  type UUID = String
  implicit val rds: Reads[Timestamp] = (__ \ "time").read[Long].map{ long: Long => new Timestamp(long) }
  implicit val wrs: Writes[Timestamp] = (__ \ "time").write[Long].contramap{ (a: Timestamp) => a.getTime }
  implicit val format: OFormat[Lst] = Json.format[Lst]
}
case class Lst(
  name: String,
  description: Option[String],
  userUuid:  String,
  uuid: Option[String] = None,
  created: Option[Timestamp] = None,
  updated: Option[Timestamp] = None
)