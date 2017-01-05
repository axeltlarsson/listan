package services

import play.api.libs.json._
import julienrf.json.derived
import models.Item

// The algebraic data type presentation of messages
// ex: {"action": {"type": "DELETE_ITEM", "id": "1a"}}
sealed trait Message
case class Auth(token: String, ack: String) extends Message
case class AuthRequest() extends Message

sealed trait Action extends Message
case class AddItem(uuid: String, contents: String, ack: String) extends Action
case class EditItem(uuid: String, contents: String, ack: String) extends Action
case class CompleteItem(uuid: String, ack: String) extends Action
case class UncompleteItem(uuid: String, ack: String) extends Action
case class DeleteItem(uuid: String, ack: String) extends Action
case class GetState(ack: String) extends Action

sealed trait Response extends Message
case class AuthResponse(status: String, ack: String) extends Response // Authorised
case class FailureResponse(error: String, ack: String) extends Response
case class UUIDResponse(status: String, uuid: Item.UUID, ack: String) extends Response
case class GetStateResponse(items: Seq[Item], ack: String) extends Response


// Json format
object Message {
  implicit val msgDataReads: Reads[Message] =
    derived.flat.reads((__ \ "type").read[String])
  implicit val msgDataWrites: OWrites[Message] =
    derived.flat.owrites((__ \ "type").write[String])
}

object Auth {
  implicit val format: OFormat[Auth] = derived.oformat
}
object AuthRequest {
  implicit val format: OFormat[AuthRequest] = derived.oformat
}

// Actions
object EditItem {
  implicit val format: OFormat[EditItem] = derived.oformat
}
object AddItem {
  implicit val format: OFormat[AddItem] = derived.oformat
}
object CompleteItem {
 implicit val format: OFormat[CompleteItem] = derived.oformat
}
object UncompleteItem {
 implicit val format: OFormat[UncompleteItem] = derived.oformat
}
object DeleteItem {
  implicit val format: OFormat[DeleteItem] = derived.oformat
}
object GetState {
 implicit val format: OFormat[GetState] = derived.oformat
}
object Action {
  implicit val format: OFormat[Action] = derived.oformat
}

// Responses
object FailureResponse {
  implicit val format: OFormat[FailureResponse] = derived.oformat
}
object UUIDResponse {
  implicit val format: OFormat[UUIDResponse] = derived.oformat
}
object AuthResponse {
  implicit val format: OFormat[AuthResponse] = derived.oformat
}
object GetStateResponse {
  implicit val format: OFormat[GetStateResponse] = derived.oformat
}
object Response {
  implicit val format: OFormat[Response] = derived.oformat
}
