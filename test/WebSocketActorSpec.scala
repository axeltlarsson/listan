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

import akka.actor.ActorSystem
import akka.pattern.ask
import akka.stream.Materializer
import javax.inject._

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


class WebSocketActorSpec extends PlaySpec with OneAppPerTest {
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
      fsm ! Json.toJson(Auth("somevalidtoken"): Message)
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

