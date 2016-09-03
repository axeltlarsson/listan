package models

import play.api.libs.json._
import play.api.libs.functional.syntax._
import java.sql.Timestamp


object Item {
  type UUID = String
  implicit val rds: Reads[Timestamp] = (__ \ "time").read[Long].map{ long => new Timestamp(long) }
  implicit val wrs: Writes[Timestamp] = (__ \ "time").write[Long].contramap{ (a: Timestamp) => a.getTime }
  implicit val format = Json.format[Item]
}
case class Item(
  contents: String,
  completed: Boolean = false,
  uuid: Option[Item.UUID] = None,
  created: Option[Timestamp] = None,
  updated: Option[Timestamp] = None
)
