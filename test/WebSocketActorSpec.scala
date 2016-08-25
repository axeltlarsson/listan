import org.scalatestplus.play._
import play.api.test._
import play.api.test.Helpers._
import play.api.libs.json._
import akka.testkit.TestFSMRef
import akka.actor._
import akka.testkit.TestActorRef
import scala.concurrent.duration._
import services._
import services.WebSocketActor._
import controllers.HomeController
import akka.actor.ActorSystem
import akka.pattern.ask
import akka.stream.Materializer
import javax.inject._
import play.api.mvc._
import scala.concurrent.Future

object MockWsActor {
  case object GetMessage
}
class MockWsActor extends Actor {
  import MockWsActor._
  var lastMsg: Any = None
  
  def receive = {
    case GetMessage => {
      sender ! lastMsg
    }
    case msg => lastMsg = msg
  }
}


class WebSocketActorSpec extends PlaySpec with OneServerPerSuite with Results {
  import MockWsActor._
  implicit val system = ActorSystem("sys")

  trait Automaton {
      val mockWsActor = TestActorRef(new MockWsActor)
      val fsm = TestFSMRef(new WebSocketActor(mockWsActor))
  }
  
  "WebSocketActor" should {
    "start in state Unauthenticated" in new Automaton {
      fsm.stateName mustBe Unauthenticated
      // This should trigger a warn log message
      fsm ! Json.toJson(EDIT_ITEM("id", "contents"): Message)
      fsm.stateName mustBe Unauthenticated
    }

    "handle valid Auth" in new Automaton {
      fsm.stateName mustBe Unauthenticated

      // Get the token by calling /api/login with creds
      val controller = new HomeController()
      val json = Json.parse("""
      {
        "username": "axel",
        "password": "whatever"
      }
      """)
      val req = new FakeRequest(
        uri = "/api/login",
        method = "POST",
        headers = FakeHeaders(Seq("Content-type"-> "application/json")),
        body = json
        )
      val Some(res) = route(app, req)
      status(res) mustEqual OK
      val token = headers(res).get("Authorization").get.split("Bearer ")(1)
      
      fsm ! Json.toJson(Auth(token): Message)
      val futureReply = mockWsActor ? GetMessage
      val result = futureReply.value.get
      result mustBe Response(true, _: String)
      fsm.stateName mustBe Authenticated
    }

    "reject invalid Auth" in new Automaton {
      fsm.stateName mustBe Unauthenticated
      // val future = fsm ? Auth("someinvalidtoken")
      // val result = future.value.get
      // println(result)
      // fsm.stateName mustBe Unauthenticated
      // ok mustBe false
    }
  }

}

