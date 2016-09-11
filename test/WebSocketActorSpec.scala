import org.scalatestplus.play._
import play.api.test._
import play.api.test.Helpers._
import play.api.libs.json._
import akka.testkit.TestFSMRef
import akka.actor._
import akka.testkit.TestActorRef
import scala.concurrent.duration._
import services._
import controllers.HomeController
import akka.actor.ActorSystem
import akka.pattern.ask
import akka.stream.Materializer
import javax.inject._
import play.api.mvc._
import scala.concurrent.Future
import play.api.inject._
import play.api.Configuration
import models.{User, UserRepository}

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
      import play.api.inject.guice.GuiceApplicationBuilder
      val app = new GuiceApplicationBuilder().build
      val injector: Injector = app.injector
      val userService = injector.instanceOf[UserService]
      val listActor: ActorRef = injector.instanceOf(BindingKey(classOf[ActorRef]).qualifiedWith("list-actor"))
      val mockWsActor = TestActorRef(new MockWsActor)
      val wsActorProvider: WebSocketActorProvider = new WebSocketActorProvider(userService, listActor)
      val fsm = TestFSMRef(wsActorProvider.get(mockWsActor))
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
      val controller = injector.instanceOf[HomeController]
      val repo = injector.instanceOf[UserRepository]
      repo.insert(User.create("axel", "whatever"))
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
      
      // Try to authenticate with the token
      fsm ! Json.toJson(Auth(token): Message)
      val futureReply = mockWsActor ? GetMessage
      val result = futureReply.value.get
      result mustBe Response(true, _: String)
      fsm.stateName mustBe Authenticated
      fsm.stateData match {
        case UserData(user) => user.name mustBe "axel"
        case _ => false mustBe true
      }
    }

    "reject invalid Auth" in new Automaton {
      // NB: This test might cause error log message (could not decode token)
      // that is entirely expected
      fsm.stateName mustBe Unauthenticated

      fsm ! Json.toJson(Auth("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiYWRtaW4iOnRydWV9.TJVA95OrM7E2cBab30RMHrHDcEfxjoYZgeFONFh7HgQ"): Message)
      val futureReply = mockWsActor ? GetMessage
      val result = futureReply.value.get
      result mustBe Response(false, _: String)
      fsm.stateName mustBe Unauthenticated
    }
  }

}

