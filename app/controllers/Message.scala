package controllers

import play.api.libs.json._
import julienrf.json.derived

case class Message(auth: String, action: Action)
object Message {
  implicit val format: OFormat[Message] = derived.oformat
}

sealed trait Action
case class EDIT_ITEM(id: String, contents: String) extends Action
case class ADD_ITEM(id: String, contents: String) extends Action
object EDIT_ITEM { implicit val format: OFormat[EDIT_ITEM] = derived.oformat }
object ADD_ITEM { implicit val format: OFormat[ADD_ITEM] = derived.oformat }

/* Must be after the sum types for some reason */
object Action {
  implicit val actionReads: Reads[Action] = derived.reads
  implicit val actionWrites: OWrites[Action] =
    derived.flat.owrites((__ \ "type").write[String])
}