package models

import java.sql.Timestamp

import julienrf.json.derived
import julienrf.json.derived.NameAdapter
import models.ItemList.UUID
import play.api.libs.functional.syntax._
import play.api.libs.json._

object Item {
  type UUID = String
  implicit val rds: Reads[Timestamp] = (__ \ "time").read[Long].map{ long => new Timestamp(long) }
  implicit val wrs: Writes[Timestamp] = (__ \ "time").write[Long].contramap{ (a: Timestamp) => a.getTime }
  implicit val format: OFormat[Item] = derived.oformat(NameAdapter.snakeCase)
}

case class Item(uuid: UUID, contents: String, completed: Boolean = false, listUuid: UUID, created: Option[Timestamp] = None, updated: Option[Timestamp] = None)
