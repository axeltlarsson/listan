import julienrf.json.derived
import julienrf.json.derived.NameAdapter
import org.scalatestplus.play._
import services._
import play.api.libs.json._
import models.{Item, ItemList}
import play.api.Logger

import scala.collection.mutable


class MessageSpec extends PlaySpec {
  val log = Logger(this.getClass)

  def jsonSerializable(msg: Message): Boolean = {
    val json = Json.toJson(msg)
    Logger.warn(Json.prettyPrint(json))
    json.validate[Message] match {
      case _: JsSuccess[Message] => true
      case _ => false
    }
  }

  "JSON serialisable" should {
    "Auth be" in {
      jsonSerializable(Auth(token = "eyJ0e...", ack = "12345..")) mustBe true
    }
    "AuthRequest be" in {
      jsonSerializable(AuthRequest()) mustBe true
    }
    "Ping be" in {
      jsonSerializable(Ping(ack = "sldjf-234s-sdf")) mustBe true
    }
    // Actions
    "AddItem be" in {
      jsonSerializable(AddItem(contents = "mjölk", list_uuid = "abc", ack = "123")) mustBe true
    }
    "EditItem be" in {
      jsonSerializable(EditItem(uuid = "sldfj-234-sdfj", contents = "filmjölk", ack = "124")) mustBe true
    }
    "CompleteItem be" in {
      jsonSerializable(CompleteItem(uuid = "124", ack = "124")) mustBe true
    }
    "UncompleteItem be" in {
      jsonSerializable(UnCompleteItem(uuid = "124", ack = "124")) mustBe true
    }
    "DeleteItem be" in {
      jsonSerializable(DeleteItem(uuid = "124", ack = "124")) mustBe true
    }
    "GetState be" in {
      jsonSerializable(GetState(ack = "124")) mustBe true
    }

    // Responses
    "AuthResponse be" in {
      jsonSerializable(AuthResponse(status = "Authorised", ack = "124")) mustBe true
    }
    "FailureResponse be" in {
      jsonSerializable(FailureResponse(error = "Naugthy, naughty!", ack = "124")) mustBe true
    }
    "UUIDResponse be" in {
      jsonSerializable(UUIDResponse(status = "Added item", uuid = "123", ack = "124")) mustBe true
    }
    "GetStateResponse be" in {
      val lists = Seq(
        ItemList(name = "list one", description = Some("list one"), userUuid = "user1", uuid = Some("abc")),
        ItemList(name = "list two", description = None, userUuid = "user1", uuid = Some("abcd"))
      )
      val items = Seq(
        Item(contents = "an item", list_uuid = "abc"),
        Item(contents = "item 2", list_uuid = "abc"),
        Item(contents = "item in other list", list_uuid = "abcd"))
      jsonSerializable(GetStateResponse(lists, items, ack = "123")) mustBe true
    }
    "Pong be" in {
      jsonSerializable(Pong(ack = "lk2j3lj")) mustBe true
    }
  }
}
