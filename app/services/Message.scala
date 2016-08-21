package services

import play.api.libs.json._
import julienrf.json.derived


// The algebraic data type presentation of message from client
// ex: {"action": {"type": "DELETE_ITEM", "id": "1a"}}
case class Message(auth: String, action: Action)

sealed trait Action
case class EDIT_ITEM(id: String, contents: String) extends Action
case class ADD_ITEM(id: String, contents: String) extends Action


/* JSON format */
object Message {
  implicit val format: OFormat[Message] = derived.oformat
}
/* Must be after the sum types for some reason */
object EDIT_ITEM { implicit val format: OFormat[EDIT_ITEM] = derived.oformat }
object ADD_ITEM { implicit val format: OFormat[ADD_ITEM] = derived.oformat }
object Action {
  implicit val actionReads: Reads[Action] =
      derived.flat.reads((__ \ "type").read[String])
  implicit val actionWrites: OWrites[Action] =
    derived.flat.owrites((__ \ "type").write[String])
}