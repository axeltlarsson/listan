package services

import play.api.libs.json._
import julienrf.json.derived
import models.Item

// The algebraic data type presentation of messages
// ex: {"action": {"type": "DELETE_ITEM", "id": "1a"}}
sealed trait Message
case class Auth(token: String) extends Message
case class AuthRequest() extends Message

sealed trait Action extends Message
case class ADD_ITEM(id: String, contents: String) extends Action
case class EDIT_ITEM(id: String, contents: String) extends Action
case class TOGGLE_ITEM(id: String) extends Action
case class DELETE_ITEM(id: String) extends Action
case class ALL() extends Action

sealed trait Response extends Message
case class FailureResponse(error: String) extends Response
case class UUIDResponse(status: String, uuid: Item.UUID) extends Response
case class StatusResponse(status: String) extends Response
case class BoolResponse(status: String, bool: Boolean) extends Response
case class AllResponse(items: Seq[Item]) extends Response


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
object EDIT_ITEM {
  implicit val format: OFormat[EDIT_ITEM] = derived.oformat
}
object ADD_ITEM {
  implicit val format: OFormat[ADD_ITEM] = derived.oformat
}
object TOGGLE_ITEM {
 implicit val format: OFormat[TOGGLE_ITEM] = derived.oformat
}
object DELETE_ITEM {
  implicit val format: OFormat[DELETE_ITEM] = derived.oformat
}
object ALL {
 implicit val format: OFormat[DELETE_ITEM] = derived.oformat
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
object StatusResponse {
  implicit val format: OFormat[UUIDResponse] = derived.oformat
}
object BoolResponse {
  implicit val format: OFormat[BoolResponse] = derived.oformat
}
object AllResponse {
  implicit val format: OFormat[AllResponse] = derived.oformat
}
object Response {
  implicit val format: OFormat[Response] = derived.oformat
}
