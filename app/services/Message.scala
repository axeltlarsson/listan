package services

import julienrf.json.derived
import julienrf.json.derived.NameAdapter
import models.{Item, ItemList}
import play.api.libs.json._

// The algebraic data type presentation of messages
// ex: {"action": {"type": "DELETE_ITEM", "id": "1a"}}
sealed trait Message
case class Auth(token: String, ack: String) extends Message
case class AuthRequest() extends Message
case class Ping(ack: String) extends Message

sealed trait Action extends Message
case class AddItem(contents: String, list_uuid: String, ack: String, uuid: Option[String] = None) extends Action // uuid for relayed msg
case class EditItem(uuid: String, contents: String, ack: String) extends Action
case class CompleteItem(uuid: String, ack: String) extends Action
case class UnCompleteItem(uuid: String, ack: String) extends Action
case class DeleteItem(uuid: String, ack: String) extends Action
case class GetState(ack: String) extends Action
case class AddList(name: String, description: Option[String], ack: String, uuid: Option[String] = None) extends Action
case class UpdateListName(uuid: String, name: String, ack: String) extends Action
case class UpdateListDescription(uuid: String, description: String, ack: String) extends Action
case class DeleteList(uuid: String, ack: String) extends Action

sealed trait Response extends Message
case class AuthResponse(status: String, ack: String) extends Response // Authorised
case class FailureResponse(error: String, ack: String) extends Response
case class UUIDResponse(status: String, uuid: String, ack: String) extends Response
case class GetStateResponse(lists: Seq[ItemList], items: Seq[Item], ack: String) extends Response
case class Ack(ack: String) extends Response // expected response to relayed Actions
case class Pong(ack: String) extends Response

/*----------------------------------------------------------------------
                      JSON format
-----------------------------------------------------------------------*/
object Message {
  implicit val msgDataReads: Reads[Message] =
    derived.flat.reads((__ \ "type").read[String])
  implicit val msgDataWrites: OWrites[Message] =
    derived.flat.owrites((__ \ "type").write[String])
}

object Auth {
  implicit val format: OFormat[Auth] = derived.oformat(NameAdapter.snakeCase)
}
object AuthRequest {
  implicit val format: OFormat[AuthRequest] = derived.oformat(NameAdapter.snakeCase)
}
object Ping {
  implicit val format: OFormat[Ping] = derived.oformat(NameAdapter.snakeCase)
}

// Actions
object EditItem {
  implicit val format: OFormat[EditItem] = derived.oformat(NameAdapter.snakeCase)
}
object AddItem {
  implicit val format: OFormat[AddItem] = derived.oformat(NameAdapter.snakeCase)
}
object CompleteItem {
 implicit val format: OFormat[CompleteItem] = derived.oformat(NameAdapter.snakeCase)
}
object UnCompleteItem {
 implicit val format: OFormat[UnCompleteItem] = derived.oformat(NameAdapter.snakeCase)
}
object DeleteItem {
  implicit val format: OFormat[DeleteItem] = derived.oformat(NameAdapter.snakeCase)
}
object GetState {
 implicit val format: OFormat[GetState] = derived.oformat(NameAdapter.snakeCase)
}
object AddList {
  implicit val format: OFormat[AddList] = derived.oformat(NameAdapter.snakeCase)
}
object UpdateListName {
  implicit val format: OFormat[UpdateListName] = derived.oformat(NameAdapter.snakeCase)
}
object UpdateListDescription {
  implicit val format: OFormat[UpdateListDescription] = derived.oformat(NameAdapter.snakeCase)
}
object DeleteList {
  implicit val format: OFormat[DeleteList] = derived.oformat(NameAdapter.snakeCase)
}

object Action {
  implicit val format: OFormat[Action] = derived.oformat(NameAdapter.snakeCase)
}

// Responses
object FailureResponse {
  implicit val format: OFormat[FailureResponse] = derived.oformat(NameAdapter.snakeCase)
}
object UUIDResponse {
  implicit val format: OFormat[UUIDResponse] = derived.oformat(NameAdapter.snakeCase)
}
object AuthResponse {
  implicit val format: OFormat[AuthResponse] = derived.oformat(NameAdapter.snakeCase)
}
object GetStateResponse {
  implicit val format: OFormat[GetStateResponse] = derived.oformat(NameAdapter.snakeCase)
}
object Ack {
  implicit val format: OFormat[Ack] = derived.oformat(NameAdapter.snakeCase)
}
object Pong {
  implicit val format: OFormat[Pong] = derived.oformat(NameAdapter.snakeCase)
}
object Response {
  implicit val format: OFormat[Response] = derived.oformat(NameAdapter.snakeCase)
}
