package models

import java.sql.Timestamp
import play.api.libs.json.{Json, OFormat, Reads, Writes}
import play.api.libs.functional.syntax._
import play.api.libs.json._

object ItemList {
  type UUID = String
  implicit val rds: Reads[Timestamp] = (__ \ "time").read[Long].map{ long: Long => new Timestamp(long) }
  implicit val wrs: Writes[Timestamp] = (__ \ "time").write[Long].contramap{ (a: Timestamp) => a.getTime }
  implicit val format: OFormat[ItemList] = Json.format[ItemList]
}

case class ItemList(
                     uuid: Option[ItemList.UUID] = None,
                     name: String,
                     description: Option[String],
                     userUuid:  User.UUID,
                     created: Option[Timestamp] = None,
                     updated: Option[Timestamp] = None
)