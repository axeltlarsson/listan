import org.scalatestplus.play._
import play.api.test._
import play.api.test.Helpers._
import play.api.libs.json._
import akka.testkit.TestFSMRef
import akka.actor._
import akka.testkit.TestProbe
import javax.inject.Singleton

import scala.concurrent.duration._
import services._
import controllers.HomeController
import akka.actor.ActorSystem
import play.api.mvc._

import scala.concurrent.{Await, ExecutionContext}
import play.api.inject._
import models.{Item, ItemList, User, UserRepository}
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll}
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import testhelpers.{EvolutionsHelper, ListHelper}

import scala.language.postfixOps

@Singleton
class WebSocketActorSpec extends PlaySpec with GuiceOneServerPerSuite with Results with BeforeAndAfter
                                          with EvolutionsHelper with ListHelper {
  override val injector = app.injector
  implicit val ec = injector.instanceOf[ExecutionContext]
  implicit val system = ActorSystem("sys")

  before {
    evolve()
  }

  after {
    clean()
  }

  /* Provides listActor, and setup to create mock wsActors */
  trait Automaton {
    val userService = injector.instanceOf[UserService]
    val listActor: ActorRef = injector.instanceOf(BindingKey(classOf[ActorRef]).qualifiedWith("list-actor"))
    val wsActorProvider = new WebSocketActorProvider(userService, listActor, ipAddress = "test-ip")

    def acquireToken(userName: String): String = {
      val json = Json.obj("username" -> userName, "password" -> "password")
      val req = FakeRequest(
        uri = "/api/login",
        method = "POST",
        headers = FakeHeaders(Seq("Content-type"-> "application/json")),
        body = json
      )
      val Some(res) = route(app, req)
      status(res) mustEqual OK
      contentAsJson(res).apply("token").as[String]
    }
  }

  /* Provide mock wsActor:s sharing an empty list */
  trait ListUsers1 {
    self: Automaton =>
    val mockWsActor = TestProbe()
    val fsm = TestFSMRef(wsActorProvider.get(mockWsActor.ref, ipAddress = "test-ip"))

    fsm.stateName mustBe Unauthenticated
    // createListUser will create a user with an empty list
    val (list1uuid, user1uuid) = createListUser("user1")(ec)

    // Get token by authenticating via user:pass credentials
    val token = acquireToken("user1")

    // Authenticate with the token
    mockWsActor.expectMsg(500 millis, Json.toJson(AuthRequest(): Message))
    fsm ! Json.toJson(Auth(token, "user1authtoken"): Message)
    private val authResponse = mockWsActor.expectMsg(500 millis,
      Json.toJson(AuthResponse("Authentication success", "user1authtoken"): Message))
    fsm.stateName mustBe Authenticated
    fsm.stateData match {
      case UserData(user) => user.name mustBe "user1"
      case _ => fail("user.name was not `user1`")
    }

    // A second client using same login
    val mockWsActor2 = TestProbe()
    val fsm2 = TestFSMRef(wsActorProvider.get(mockWsActor2.ref, ipAddress = "test-ip2"))
    mockWsActor2.expectMsg(500 millis, Json.toJson(AuthRequest(): Message))
    fsm2 ! Json.toJson(Auth(token, "user1auth2token"): Message)
    mockWsActor2.expectMsg(500 millis, Json.toJson(AuthResponse("Authentication success", "user1auth2token"): Message))
  }

  /* Provides a new user `user2` and client `mockWsActor3` with a separate empty list */
  trait List2User {
    self: Automaton =>
    val mockWsActor3 = TestProbe()
    val fsm3 = TestFSMRef(wsActorProvider.get(mockWsActor3.ref, ipAddress = "test-ip3"))
    val (list2uuid, user2uuid) = createListUser("user2")(ec)
    val token2 = acquireToken("user2")
    // Authenticate user with token
    mockWsActor3.expectMsg(500 millis, Json.toJson(AuthRequest(): Message))
    fsm3 ! Json.toJson(Auth(token2, "user2authtoken"): Message)
    private val authResponse = mockWsActor3.expectMsg(500 millis,
      Json.toJson(AuthResponse("Authentication success", "user2authtoken"): Message))
    fsm3.stateName mustBe Authenticated
    fsm3.stateData match {
      case UserData(user) => user.name mustBe "user2"
      case _ => fail("user.name was not `user2`")
    }
  }

  "WebSocketActor" should {
    "handle AddItem and DeleteItem" in new Automaton with ListUsers1 {
      fsm ! Json.toJson(AddItem("some contents that is to be added", list_uuid = list1uuid, "someAckNbr"): Message)
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

    "respond to Ping with Pong" in new Automaton with ListUsers1 {
      fsm ! Json.toJson(Ping(ack = "pong-me"): Message)
      val pongAck = for {
        pong <- Option(mockWsActor.receiveOne(500 millis)).map(_.asInstanceOf[JsObject])
        p <- pong.validate[Pong].asOpt
      } yield p.ack
      pongAck mustBe defined
      pongAck.get mustBe "pong-me"
    }
  }

  "System with multiple clients and same user" should {

    "handle AddItem, EditItem, DeleteItem" in new Automaton with ListUsers1 {
      /* Add item */
      fsm ! Json.toJson(AddItem("some contents that is to be added", list_uuid = list1uuid, "ackNbr"): Message)
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
      fsm2 ! Json.toJson(AddItem("extra item", list_uuid = list1uuid, "extra-ack-nbr"): Message)
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
      fsm2 ! Json.toJson(UnCompleteItem(uuidExtraAdd, "uncomplete-ack"): Message)
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
      val lists = (resState \ "lists").as[Seq[(ItemList, Seq[Item])]]
      // the items list should contain the correct items
      lists must have length 1
      val (itemList, items) = lists(0)
      items(0).contents must (be ("changed content") or be ("extra item"))
      items(0).contents must not be (items(1).contents)
      items(1).contents must (be ("changed content") or be ("extra item"))
      items(0).completed must not be (items(1).completed)
      itemList.description mustBe None
      itemList.name mustBe "a list for user1" // as per ListHelper

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

    "handle adding, editing and deleting lists" in new Automaton with ListUsers1 {
      // Add a list
      val addList = AddList(name = "a new list", description = None, ack = "msg1"): Message
      fsm ! Json.toJson(addList)
      expectResponse(mockWsActor, "UUIDResponse", "Added list", "msg1")

      // expect extra client to get the relayed AddList
      expectRelayedMessage(mockWsActor2, addList)

      // Update name
      val updateName = UpdateListName(name = "new name", uuid = list1uuid, ack = "update name"): Message
      fsm ! Json.toJson(updateName)
      expectResponse(mockWsActor, "UUIDResponse", "Updated list name", "update name")
      val resUpdate = mockWsActor.receiveOne(500 millis).asInstanceOf[JsObject]

      // expect extra client to receive relayed UpdateListName
      expectRelayedMessage(mockWsActor2, updateName)

      // Update list description
      val updateListDescr = UpdateListDescription(description = "new name", uuid = list1uuid, ack = "update name"): Message
      fsm ! Json.toJson(updateListDescr)
      expectResponse(mockWsActor, "UUIDResponse", "Updated list description", "update name")

      // expect extra client to receive relayed UpdateListDescription
      expectRelayedMessage(mockWsActor2, updateListDescr)

      // Delete list
      val deleteList = DeleteList(uuid = list1uuid, ack = "delete list"): Message
      fsm ! Json.toJson(deleteList)
      expectResponse(mockWsActor, "UUIDResponse", "Deleted list", "delete list")

      // expect extra client to recieve relayed DeleteList
      expectRelayedMessage(mockWsActor2, deleteList)
    }
  }

  "System with multiple clients and users" should {
    "not allow users to access other users' lists" in new Automaton with ListUsers1 with List2User {
      // Update list name
      val updateName = UpdateListName(name = "new name", uuid = list1uuid, ack = "msg1"): Message
      fsm ! Json.toJson(updateName)
      expectResponse(mockWsActor, "UUIDResponse", "Updated list name", "msg1")

      // client 2 should receive relayed msg...
      expectRelayedMessage(mockWsActor2, updateName)

      // ... but not client 3 which is user2
      val msg = mockWsActor3.receiveOne(500 millis).asInstanceOf[JsObject]
      msg mustBe null

    }
  }

  def expectResponse(to: TestProbe, typeStr: String, status: String, ack: String) = {
    val result = to.receiveOne(500 millis).asInstanceOf[JsObject]
    assertMessage(result)
    (result \ "type").as[String] mustBe typeStr
    (result \ "status").as[String] mustBe status
    (result \ "ack").as[String] mustBe ack
  }

  def expectRelayedMessage(to: TestProbe, msg: Message) = {
    val result = to.receiveOne(500 millis).asInstanceOf[JsObject]
    assertMessage(result)
    val msgAsJson  = Json.toJson(msg)
    msgAsJson mustBe result
  }

  def assertMessage(json: JsObject): Unit = {
    assert(json != null, "json was null!")
    json.validate[Message] match {
      case yes: JsSuccess[Message] => true mustBe true
      case JsError(errors) => {
        fail(s"Json was not Message: $json")
      }
    }
  }

}
