package models

import java.sql.Timestamp

import julienrf.json.derived
import julienrf.json.derived.NameAdapter
import models.User.UUID
import play.api.libs.functional.syntax._
import play.api.libs.json.{OFormat, Reads, Writes, _}

object ItemList {
  type UUID = String
  implicit val rds: Reads[Timestamp] = (__ \ "time").read[Long].map{ long: Long => new Timestamp(long) }
  implicit val wrs: Writes[Timestamp] = (__ \ "time").write[Long].contramap{ (a: Timestamp) => a.getTime }
  implicit val format: OFormat[ItemList] = derived.oformat(NameAdapter.snakeCase)
}

case class ItemList(uuid: UUID, name: String, description: Option[String], userUuid: UUID, created: Option[Timestamp] = None, updated: Option[Timestamp] = None)