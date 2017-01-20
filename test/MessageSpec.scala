import org.scalatestplus.play._
import play.api.test._
import play.api.test.Helpers._
import services._
import play.api.libs.json._
import play.api.Logger
import models.Item

class MessageSpec extends PlaySpec with OneAppPerTest {

  def jsonSerializable(msg: Message): Boolean = {
    val json = Json.toJson(msg)
    // Logger.debug(Json.prettyPrint(json))
    json.validate[Message] match {
      case s: JsSuccess[Message] => true
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
      jsonSerializable(AddItem(contents = "mjölk", ack = "123")) mustBe true
    }
    "EditItem be" in {
      jsonSerializable(EditItem(uuid = "sldfj-234-sdfj", contents = "filmjölk", ack = "124")) mustBe true
    }
    "CompleteItem be" in {
      jsonSerializable(CompleteItem(uuid = "124", ack = "124")) mustBe true
    }
    "UncompleteItem be" in {
      jsonSerializable(UncompleteItem(uuid = "124", ack = "124")) mustBe true
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
      jsonSerializable(GetStateResponse(items = Seq(Item("mjölk"), Item("filmjölk")), ack = "123")) mustBe true
    }
    "Pong be" in {
      jsonSerializable(Pong(ack = "lk2j3lj")) mustBe true
    }
  }
}
