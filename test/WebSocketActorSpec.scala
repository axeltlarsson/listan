import org.scalatestplus.play._
import play.api.test._
import play.api.test.Helpers._
import play.api.libs.json._
import akka.testkit.TestFSMRef
import akka.actor._
import akka.testkit.TestActorRef
import scala.concurrent.duration._
import services._

import akka.actor.ActorSystem
import akka.stream.Materializer
import javax.inject._
 
class MockWsActor extends Actor {
  def receive = {
    case msg => println("received: " + msg)
  }
}

@Singleton
class WebSocketActorSpec @Inject() (implicit sys: ActorSystem, mat: Materializer)
  extends PlaySpec with OneAppPerTest {
/*  "WebSocketActor" should {
    "handle state transitions" in {
      val fsm = TestFSMRef(new WebSocketActor(TestActorRef(new MockWsActor)))
      fsm.stateName mustBe "hej"
    }
  }*/

  "true mustBe true" in  {
    true mustBe true
  }
}

