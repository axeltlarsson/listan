import org.scalatestplus.play._
import play.api.test._
import play.api.test.Helpers._
import play.api.libs.json._
import akka.testkit.TestFSMRef
import akka.actor._
import akka.testkit.{TestActorRef, TestProbe}
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
import scala.language.postfixOps
import play.api.Logger

class WebSocketActorSpec extends PlaySpec with OneServerPerSuite with Results {
  implicit val system = ActorSystem("sys")
  var token = ""

  trait Automaton {
      import play.api.inject.guice.GuiceApplicationBuilder
      val app = new GuiceApplicationBuilder().build
      val injector: Injector = app.injector
      val userService = injector.instanceOf[UserService]
      val listActor: ActorRef = injector.instanceOf(BindingKey(classOf[ActorRef]).qualifiedWith("list-actor"))
      val mockWsActor = TestProbe()
      val wsActorProvider = new WebSocketActorProvider(userService, listActor, ipAddress = "test-ip")
      val fsm = TestFSMRef(wsActorProvider.get(mockWsActor.ref, ipAddress = "test-ip"))
  }

  trait ExtraClient {
    self: Automaton =>
    val mockWsActor2 = TestProbe()
    val fsm2 = TestFSMRef(wsActorProvider.get(mockWsActor2.ref, ipAddress = "test-ip2"))
    mockWsActor2.expectMsg(500 millis, Json.toJson(AuthRequest(): Message))
    fsm2 ! Json.toJson(Auth(token, "12345"): Message)
    mockWsActor2.expectMsg(500 millis, Json.toJson(AuthResponse("Authentication success", "12345"): Message))
  }

  trait Authenticated {
    self: Automaton =>
    if (token == "") {
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
      token = headers(res).get("Authorization").get.split("Bearer ")(1)
    }

    // Try to authenticate with the token
    mockWsActor.expectMsg(500 millis, Json.toJson(AuthRequest(): Message))
    fsm ! Json.toJson(Auth(token, "ackNbr"): Message)
    mockWsActor.expectMsg(500 millis, Json.toJson(AuthResponse("Authentication success", "ackNbr"): Message))
    fsm.stateName mustBe Authenticated
    fsm.stateData match {
      case UserData(user) => user.name mustBe "axel"
      case _ => fail("user.name was not axel")
    }
  }

  "WebSocketActor" should {
    "start in state Unauthenticated" in new Automaton {
      fsm.stateName mustBe Unauthenticated
      // This should trigger a warn log message
      fsm.stateName mustBe Unauthenticated
      fsm ! Json.toJson(EditItem(uuid = "id", contents = "contents", ack = "123"): Message)
    }

    "reject invalid Auth" in new Automaton {
      // NB: This test might cause error log message (could not decode token)
      // that is entirely expected
      fsm.stateName mustBe Unauthenticated
      mockWsActor.expectMsg(500 millis, Json.toJson(AuthRequest(): Message))
      fsm ! Json.toJson(Auth("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiYWRtaW4iOnRydWV9.TJVA95OrM7E2cBab30RMHrHDcEfxjoYZgeFONFh7HgQ", "1234"): Message)
      mockWsActor.expectMsg(500 millis, Json.toJson(FailureResponse("Authentication failure", "1234"): Message))
    }

    "handle AddItem and DeleteItem" in new Automaton with Authenticated {
      fsm ! Json.toJson(AddItem("some contents that is to be added", "someAckNbr"): Message)
      val res = mockWsActor.receiveOne(500 millis).asInstanceOf[JsObject]
      // Must be some way to make the following code compose better
      res.validate[Message] match {
        case uuidRes: JsSuccess[Message] => {
          val uuid = uuidRes.get.asInstanceOf[UUIDResponse].uuid
          val ack = uuidRes.get.asInstanceOf[UUIDResponse].ack
          ack mustBe "someAckNbr"
          fsm ! Json.toJson(DeleteItem(uuid, "ackNbr"): Message)
          val resDel = mockWsActor.receiveOne(500 millis).asInstanceOf[JsObject]
          resDel.validate[Message] match {
            case res: JsSuccess[Message] =>
              res.get.asInstanceOf[UUIDResponse].status mustBe "Deleted item"
            case JsError(_) => fail("Could not validate JSON as Message")
          }
        }
        case JsError(errors) => fail("Could not validate JSON as Message")
      }
    }

    "respond to Ping with Pong" in new Automaton with Authenticated {
      fsm ! Json.toJson(Ping(ack = "pong-me"): Message)
      val pongAck = for {
        pong <- Option(mockWsActor.receiveOne(500 millis)).map(_.asInstanceOf[JsObject])
        p <- pong.validate[Pong].asOpt
      } yield p.ack
      pongAck mustBe defined
      pongAck.get mustBe "pong-me"
    }
  }

  "System with multiple clients" should {

    "handle AddItem, EditItem, DeleteItem" in new Automaton with Authenticated with ExtraClient {
      /* Add item */
      fsm ! Json.toJson(AddItem("some contents that is to be added", "ackNbr"): Message)
      // mockWsActor should get UUIDResponse
      val resAdd = mockWsActor.receiveOne(500 millis).asInstanceOf[JsObject]
      assertMessage(resAdd)
      val uuid = (resAdd \ "uuid").as[String]
      (resAdd \ "ack").as[String] mustBe "ackNbr"
      // mockWsActor2 should get relayed AddItem action with uuid set
      val relayedAdd = mockWsActor2.receiveOne(500 millis).asInstanceOf[JsObject]
      assertMessage(relayedAdd)
      (relayedAdd \ "uuid").as[String] mustBe uuid

      /* Add extra item from mockWsActor2 */
      fsm2 ! Json.toJson(AddItem("extra item", "extra-ack-nbr"): Message)
      // mockWsActor2 should get UUIDResponse for "extra item"
      val resExtraAdd = mockWsActor2.receiveOne(500 millis).asInstanceOf[JsObject]
      assertMessage(resExtraAdd)
      (resExtraAdd \ "ack").as[String] mustBe "extra-ack-nbr"
      val uuidExtraAdd = (resExtraAdd \ "uuid").as[String]
      uuidExtraAdd must not be (uuid)
      // mockWsActor should get relayed AddItem action
      val relayedExtraAdd = mockWsActor.receiveOne(500 millis).asInstanceOf[JsObject]
      assertMessage(relayedExtraAdd)
      (relayedExtraAdd \ "uuid").as[String] mustBe uuidExtraAdd

      /* mockWsActor2 sends EditItem */
      fsm2 ! Json.toJson(EditItem(uuid, "changed content", "13-234-sjd-234"): Message)
      // mockWsActor2 should get proper UUIDResponse
      val resEdit = mockWsActor2.receiveOne(500 millis).asInstanceOf[JsObject]
      assertMessage(resEdit)
      (resEdit \ "uuid").as[String] mustBe uuid
      (resEdit \ "ack").as[String] mustBe "13-234-sjd-234"
      // mockWsActor should get relayed EditItem action
      val relayedEdit = mockWsActor.receiveOne(500 millis).asInstanceOf[JsObject]
      (relayedEdit \ "uuid").as[String] mustBe uuid
      (relayedEdit \ "ack").as[String] mustBe "13-234-sjd-234"

      /* mockWsActor sends CompleteItem */
      fsm ! Json.toJson(CompleteItem(uuid, "ack-nbr"): Message)
      // mockWsActor should get UUIDResponse
      val resComplete = mockWsActor.receiveOne(500 millis).asInstanceOf[JsObject]
      assertMessage(resComplete)
      (resComplete \ "uuid").as[String] mustBe uuid
      (resComplete \ "ack").as[String] mustBe "ack-nbr"
      (resComplete \ "status").as[String] mustBe "Completed item"
      // mockWsActor2 should get relayed CompleteItem action
      val relayedComplete = mockWsActor2.receiveOne(500 millis).asInstanceOf[JsObject]
      assertMessage(relayedComplete)
      (relayedComplete \ "uuid").as[String] mustBe uuid

      /* mockWsActor2 sends UncompleteItem for extra item */
      fsm2 ! Json.toJson(UncompleteItem(uuidExtraAdd, "uncomplete-ack"): Message)
      // mockWsActor2 should get UUIDResponse
      val resUncomplete = mockWsActor2.receiveOne(500 millis).asInstanceOf[JsObject]
      assertMessage(resUncomplete)
      (resUncomplete \ "uuid").as[String] mustBe uuidExtraAdd
      (resUncomplete \ "ack").as[String] mustBe "uncomplete-ack"
      (resUncomplete \ "status").as[String] mustBe "Uncompleted item"
      // mockWsActor should get relayed UncompleteItem action
      val relayedUncomplete = mockWsActor.receiveOne(500 millis).asInstanceOf[JsObject]
      assertMessage(relayedUncomplete)
      (relayedUncomplete \ "uuid").as[String] mustBe uuidExtraAdd

      /* Let mockWsActor send GetState */
      fsm ! Json.toJson(GetState("get-state-ack"): Message)
      // mockWsActor should get GetStateResponse
      val resState = mockWsActor.receiveOne(500 millis).asInstanceOf[JsObject]
      assertMessage(resState)
      val items = (resState \ "items").as[Seq[models.Item]]
      // the items list should contain the correct items
      items must have length 2
      items(0).contents must (be ("changed content") or be ("extra item"))
      items(0).contents must not be (items(1).contents)
      items(1).contents must (be ("changed content") or be ("extra item"))
      items(0).completed must not be (items(1).completed)

      /* Let mockWsActor1 send DeleteItem */
      fsm ! Json.toJson(DeleteItem(uuid, "ack"): Message)
      // mockWsActor should get proper UUIDResponse
      val resDel = mockWsActor.receiveOne(500 millis).asInstanceOf[JsObject]
      assertMessage(resDel)
      (resDel \ "uuid").as[String] mustBe uuid
      (resDel \ "ack").as[String] mustBe "ack"
      // mockWsActor2 should get relayed DeleteItem action
      val relayedDel = mockWsActor2.receiveOne(500 millis).asInstanceOf[JsObject]
      assertMessage(relayedDel)
      (relayedDel \ "uuid").as[String] mustBe uuid

      /* Let mockWsActor1 send DeleteItem for Extra Item */
      fsm ! Json.toJson(DeleteItem(uuidExtraAdd, "ack"): Message)
      // mockWsActor should get proper UUIDResponse
      val resDelExtra = mockWsActor.receiveOne(500 millis).asInstanceOf[JsObject]
      assertMessage(resDelExtra)
      (resDelExtra \ "uuid").as[String] mustBe uuidExtraAdd
      (resDelExtra \ "ack").as[String] mustBe "ack"
      // mockWsActor2 should get relayed DeleteItem action
      val relayedDelExtra = mockWsActor2.receiveOne(500 millis).asInstanceOf[JsObject]
      assertMessage(relayedDelExtra)
      (relayedDelExtra \ "uuid").as[String] mustBe uuidExtraAdd
    }

  }

  def assertMessage(json: JsObject): Unit = {
    json.validate[Message] match {
      case yes: JsSuccess[Message] => true mustBe true
      case JsError(errors) => {
        fail(s"Json was not Message: $json")
      }
    }
  }

}
