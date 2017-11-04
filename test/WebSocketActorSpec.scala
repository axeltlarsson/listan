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
      val addItem = AddItem("some contents that is to be added", listUuid = list1uuid, "some ack nbr"): Message
      fsm ! Json.toJson(addItem)
      val uuid = expectUUIDResponse(mockWsActor, "Added item", ack = "some ack nbr")

      val deleteItem = DeleteItem(uuid, "delete item ack nbr"): Message
      fsm ! Json.toJson(deleteItem)
      expectUUIDResponse(mockWsActor, "Deleted item", ack = "delete item ack nbr")
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
      val addItem = AddItem("some contents that is to be added", listUuid = list1uuid, "ackNbr"): Message
      fsm ! Json.toJson(addItem)
      // mockWsActor should get UUIDResponse
      val uuid = expectUUIDResponse(mockWsActor, "Added item", "ackNbr")
      // mockWsActor2 should get relayed AddItem action with uuid set
      val relayedAdd = expectRelayedMessage(mockWsActor2, addItem)
      (relayedAdd \ "uuid").as[String] mustBe uuid

      /* Add extra item from mockWsActor2 */
      val addItem2 = AddItem("extra item", listUuid = list1uuid, "extra-ack-nbr"): Message
      fsm2 ! Json.toJson(addItem2)
      // mockWsActor2 should get UUIDResponse for "extra item"
      val uuid2 = expectUUIDResponse(mockWsActor2, "Added item", ack = "extra-ack-nbr")
      uuid2 must not be (uuid)
      // mockWsActor should get relayed AddItem action
      val relayedExtraAdd = expectRelayedMessage(mockWsActor, addItem2)
      (relayedExtraAdd \ "uuid").as[String] mustBe uuid2

      /* mockWsActor2 sends EditItem */
      val editItem = EditItem(uuid, "changed content", "13-234-sjd-234"): Message
      fsm2 ! Json.toJson(editItem)
      // mockWsActor2 should get proper UUIDResponse
      val uuid1Again = expectUUIDResponse(mockWsActor2, "Edited item", ack = "13-234-sjd-234")
      uuid1Again mustBe uuid
      // mockWsActor should get relayed EditItem action
      val relayedEdit = expectRelayedMessage(mockWsActor, editItem)
      (relayedEdit \ "uuid").as[String] mustBe uuid

      /* mockWsActor sends CompleteItem */
      val completeItem = CompleteItem(uuid, "ack-nbr"): Message
      fsm ! Json.toJson(completeItem)
      // mockWsActor should get UUIDResponse
      val uuid1AgainAgain = expectUUIDResponse(mockWsActor, "Completed item", "ack-nbr")
      uuid1AgainAgain mustBe uuid
      // mockWsActor2 should get relayed CompleteItem action
      val relayedComplete = expectRelayedMessage(mockWsActor2, completeItem)
      (relayedComplete \ "uuid").as[String] mustBe uuid

      /* mockWsActor2 sends UnCompleteItem for extra item */
      val unCompleteItem = UnCompleteItem(uuid2, "uncomplete-ack"): Message
      fsm2 ! Json.toJson(unCompleteItem)
      // mockWsActor2 should get UUIDResponse
      val uuidUncomplete = expectUUIDResponse(mockWsActor2, "Uncompleted item", "uncomplete-ack")
      uuidUncomplete mustBe uuid2

      // mockWsActor should get relayed UncompleteItem action
      val relayedUncomplete = expectRelayedMessage(mockWsActor, unCompleteItem)
      (relayedUncomplete \ "uuid").as[String] mustBe uuid2

      /* Let mockWsActor send GetState */
      fsm ! Json.toJson(GetState("get-state-ack"): Message)
      // mockWsActor should get GetStateResponse
      val resState = mockWsActor.receiveOne(500 millis).asInstanceOf[JsObject]
      assertMessage(resState)
      val lists = (resState \ "lists").as[Seq[ItemList]]
      // the items list should contain the correct items
      lists must have length 1
      val itemList = lists(0)
      val items = (resState \ "items").as[Seq[Item]]
      items(0).contents must (be ("changed content") or be ("extra item"))
      items(0).contents must not be (items(1).contents)
      items(1).contents must (be ("changed content") or be ("extra item"))
      items(0).completed must not be (items(1).completed)
      itemList.description mustBe None
      itemList.name mustBe "a list for user1" // as per ListHelper

      /* Let mockWsActor1 send DeleteItem */
      val deleteItem = DeleteItem(uuid, "delete ack"): Message
      fsm ! Json.toJson(deleteItem)
      // mockWsActor should get proper UUIDResponse
      val resDel = expectUUIDResponse(mockWsActor, "Deleted item", ack = "delete ack")
      resDel mustBe uuid
      // mockWsActor2 should get relayed DeleteItem action
      val relayedDel = expectRelayedMessage(mockWsActor2, deleteItem)
      (relayedDel \ "uuid").as[String] mustBe uuid

      /* Let mockWsActor1 send DeleteItem for Extra Item */
      val deleteItem2 = DeleteItem(uuid2, "delete extra item ack"): Message
      fsm ! Json.toJson(deleteItem2)
      // mockWsActor should get proper UUIDResponse
      val resDelExtra = expectUUIDResponse(mockWsActor, "Deleted item", ack = "delete extra item ack")
      resDelExtra mustBe uuid2
      // mockWsActor2 should get relayed DeleteItem action
      val relayedDelExtra = expectRelayedMessage(mockWsActor2, deleteItem2)
      (relayedDelExtra \ "uuid").as[String] mustBe uuid2
    }

    "handle adding, editing and deleting lists" in new Automaton with ListUsers1 {
      // Add a list
      val addList = AddList(name = "a new list", description = None, ack = "msg1"): Message
      fsm ! Json.toJson(addList)
      expectUUIDResponse(mockWsActor, "Added list", "msg1")

      // expect extra client to get the relayed AddList
      expectRelayedMessage(mockWsActor2, addList)

      // Update name
      val updateName = UpdateListName(name = "new name", uuid = list1uuid, ack = "update name"): Message
      fsm ! Json.toJson(updateName)
      expectUUIDResponse(mockWsActor, "Updated list name", "update name")
      val resUpdate = mockWsActor.receiveOne(500 millis).asInstanceOf[JsObject]

      // expect extra client to receive relayed UpdateListName
      expectRelayedMessage(mockWsActor2, updateName)

      // Update list description
      val updateListDescr = UpdateListDescription(description = "new name", uuid = list1uuid, ack = "update descr"): Message
      fsm ! Json.toJson(updateListDescr)
      expectUUIDResponse(mockWsActor, "Updated list description", "update descr")

      // expect extra client to receive relayed UpdateListDescription
      expectRelayedMessage(mockWsActor2, updateListDescr)

      // Delete list
      val deleteList = DeleteList(uuid = list1uuid, ack = "delete list"): Message
      fsm ! Json.toJson(deleteList)
      expectUUIDResponse(mockWsActor, "Deleted list", "delete list")

      // expect extra client to receive relayed DeleteList
      expectRelayedMessage(mockWsActor2, deleteList)
    }

    "gracefully handle trying to delete/edit non-existent list" in new Automaton with ListUsers1 {
      // try to delete
      val deleteList = DeleteList(uuid = "i do not exist", ack = "delete non-existent list"): Message
      fsm ! Json.toJson(deleteList)
      val response = mockWsActor.receiveOne(500 millis).asInstanceOf[JsObject]
      response("type").as[String] mustBe "FailureResponse"
      response("error").as[String] mustBe "An error ocurred, see server logs"
      response("ack").as[String] mustBe "delete non-existent list"
      // mockWsActor should NOT get this error relayed
      mockWsActor2.receiveOne(500 millis) mustBe null

      // try to change name
      val editName = UpdateListName(uuid = "i do not exist", name = "update name", ack = "update name"): Message
      fsm ! Json.toJson(editName)
      val response2 = mockWsActor.receiveOne(500 millis).asInstanceOf[JsObject]
      response2("type").as[String] mustBe "FailureResponse"
      response2("error").as[String] mustBe "An error ocurred, see server logs"
      response2("ack").as[String] mustBe "update name"
      // mockWsActor should NOT get this error relayed
      mockWsActor2.receiveOne(500 millis) mustBe null

      // try to change description
      val updateDescr = UpdateListDescription("no real uuid", "descr", "update name ack"): Message
      fsm ! Json.toJson(updateDescr)
      val response3 = mockWsActor.receiveOne(500 millis).asInstanceOf[JsObject]
      response3("type").as[String] mustBe "FailureResponse"
      response3("error").as[String] mustBe "An error ocurred, see server logs"
      response3("ack").as[String] mustBe "update name ack"
      // mockWsActor should NOT get this error relayed
      mockWsActor2.receiveOne(500 millis) mustBe null
    }
  }

  "System with multiple clients and users" should {
    "not allow users to access other users' lists" in new Automaton with ListUsers1 with List2User {
      // Update list name
      val updateName = UpdateListName(name = "new name", uuid = list1uuid, ack = "msg1"): Message
      fsm ! Json.toJson(updateName)
      expectUUIDResponse(mockWsActor, "Updated list name", "msg1")

      // client 2 should receive relayed msg...
      expectRelayedMessage(mockWsActor2, updateName)

      // ... but not client 3 which is user2
      val msg = mockWsActor3.receiveOne(500 millis).asInstanceOf[JsObject]
      msg mustBe null

      // Update list description from user2
      val updateDescr = UpdateListDescription(uuid = list2uuid, description = "a new description", ack = "descr")
      fsm3 ! Json.toJson(updateDescr: Message)
      expectUUIDResponse(mockWsActor3, "Updated list description", "descr")

      // neither client 1 or 2 should receive any information regarding this
      val msg1 = mockWsActor.receiveOne(500 millis).asInstanceOf[JsObject]
      msg1 mustBe null
      val msg2 = mockWsActor2.receiveOne(500 millis).asInstanceOf[JsObject]
      msg2 mustBe null
    }

    "not allow users access to other users' items" in new Automaton with ListUsers1 with List2User {
      // Let user1 clients (mockWsActor1 and mockWsActor2) add, edit, delete items in list1
      val addItem1 = AddItem("a new item", list1uuid, ack = "item1"): Message
      fsm ! Json.toJson(addItem1)
      val uuid1 = expectUUIDResponse(mockWsActor, "Added item", "item1")
      expectRelayedMessage(mockWsActor2, addItem1)
      val msg1 = mockWsActor3.receiveOne(500 millis).asInstanceOf[JsObject]
      msg1 mustBe null
      val completeItem1 = CompleteItem(uuid1, ack = "complete item 1"): Message
      fsm ! Json.toJson(completeItem1)
      expectUUIDResponse(mockWsActor, "Completed item", "complete item 1")
      val msg2 = mockWsActor3.receiveOne(500 millis).asInstanceOf[JsObject]
      msg2 mustBe null
    }
  }

  def expectUUIDResponse(to: TestProbe, status: String, ack: String): String = {
    val result = to.receiveOne(500 millis).asInstanceOf[JsObject]
    assertMessage(result)
    (result \ "type").as[String] mustBe "UUIDResponse"
    (result \ "status").as[String] mustBe status
    (result \ "ack").as[String] mustBe ack

    (result \ "uuid").as[String]
  }

  def expectRelayedMessage(to: TestProbe, msg: Message): JsObject = {
    val result = to.receiveOne(500 millis).asInstanceOf[JsObject]
    assertMessage(result)
    val msgAsJson  = Json.toJson(msg).as[JsObject]
    if ((msgAsJson \ "uuid").isDefined) {
      msgAsJson mustBe result
    } else {
      // merge in the uuid field
      val uuid = "uuid" -> result("uuid")
      (msgAsJson + uuid) mustBe result
    }
    result
  }

  def assertMessage(json: JsObject): Unit = {
    json must not be null
    json.validate[Message] match {
      case _: JsSuccess[Message] => true mustBe true
      case JsError(_) => {
        fail(s"Json was not Message: $json")
      }
    }
  }

}
