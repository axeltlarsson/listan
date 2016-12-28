import org.scalatestplus.play._
import play.api.test._
import play.api.test.Helpers._
import services._
import play.api.libs.json._

class MessageSpec extends PlaySpec with OneAppPerTest {

  def jsonSerializable(msg: Message): Boolean = {
    val json = Json.toJson(msg)
    println(json)
    json.validate[Message] match {
      case s: JsSuccess[Message] => true
      case _ => false
    }
  }

  "Message as json" should {

    "work for action messages" in {
      jsonSerializable(EDIT_ITEM("id1", "sdf")) mustBe true
      jsonSerializable(ADD_ITEM("id2", "sdf")) mustBe true
    }

    "work for auth messages" in {
      jsonSerializable(Auth("secret")) mustBe true
    }
  }

  "(Out) Message as json" should {
    "work for request_auth messages" in {
      jsonSerializable(AuthRequest(): Message) mustBe true
    }

    "work for response messages" in {
      jsonSerializable(StatusResponse("Unauthorized")) mustBe true
    }

    "work for AuthResponse" in {
      jsonSerializable(AuthResponse("Unauthorized"): Message) mustBe true
    }
  }
}
