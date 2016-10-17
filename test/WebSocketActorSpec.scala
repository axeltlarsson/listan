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
      val wsActorProvider: WebSocketActorProvider = new WebSocketActorProvider(userService, listActor)
      val fsm = TestFSMRef(wsActorProvider.get(mockWsActor.ref))
  }

  trait ExtraClient {
    self: Automaton =>
    val mockWsActor2 = TestProbe()
    val fsm2 = TestFSMRef(wsActorProvider.get(mockWsActor2.ref))
    mockWsActor2.expectMsg(500 millis, Json.toJson(AuthRequest(): Message))
    fsm2 ! Json.toJson(Auth(token): Message)
    mockWsActor2.expectMsg(500 millis, Json.toJson(StatusResponse("Authentication success"): StatusResponse))
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
    fsm ! Json.toJson(Auth(token): Message)
    mockWsActor.expectMsg(500 millis, Json.toJson(StatusResponse("Authentication success"): StatusResponse))
    fsm.stateName mustBe Authenticated
    fsm.stateData match {
      case UserData(user) => user.name mustBe "axel"
      case _ => false mustBe true
    }
  }
  
  "WebSocketActor" should {
    "start in state Unauthenticated" in new Automaton {
      fsm.stateName mustBe Unauthenticated
      // This should trigger a warn log message
      fsm ! Json.toJson(EDIT_ITEM("id", "contents"): Message)
      fsm.stateName mustBe Unauthenticated
    }

    "reject invalid Auth" in new Automaton {
      // NB: This test might cause error log message (could not decode token)
      // that is entirely expected
      fsm.stateName mustBe Unauthenticated
      mockWsActor.expectMsg(500 millis, Json.toJson(AuthRequest(): Message))
      fsm ! Json.toJson(Auth("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiYWRtaW4iOnRydWV9.TJVA95OrM7E2cBab30RMHrHDcEfxjoYZgeFONFh7HgQ"): Message)
      mockWsActor.expectMsg(500 millis, Json.toJson(FailureResponse("Authentication failure"): FailureResponse))
      fsm.stateName mustBe Unauthenticated
    }

    "handle ADD_ITEM and DELETE_ITEM" in new Automaton with Authenticated {
      fsm ! Json.toJson(ADD_ITEM("some id", "some contents that is to be added"): Message)
      val res = mockWsActor.receiveOne(500 millis).asInstanceOf[JsObject]
      // Must be some way to make the following code compose better
      res.validate[Response] match {
        case uuidRes: JsSuccess[Response] => {
          val uuid = uuidRes.get.asInstanceOf[UUIDResponse].uuid
          fsm ! Json.toJson(DELETE_ITEM(uuid): Message)
          val resDel = mockWsActor.receiveOne(500 millis).asInstanceOf[JsObject]
          resDel.validate[Response] match {
            case res: JsSuccess[Response] =>
              res.get.asInstanceOf[StatusResponse].status mustBe "Deleted item"
            case JsError(_) => false mustBe true
          }
        }
        case JsError(errors) => false mustBe true
      }
    }
  }

  "System with multiple clients" should {

    // TODO: clean up after, i.d. delete the item we add
    "handle ADD_ITEM" in new Automaton with Authenticated with ExtraClient {
      fsm ! Json.toJson(ADD_ITEM("some id", "some contents that is to be added"): Message)
      val res = mockWsActor.receiveOne(500 millis).asInstanceOf[JsObject]
      res.validate[Response] match {
        case yes: JsSuccess[Response] => true mustBe true
        case JsError(errors) => false mustBe true
      }
      val res2 = mockWsActor2.receiveOne(500 millis).asInstanceOf[JsObject]
      // Todo validate the action better (more than just checking that is an Action)
      res2.validate[services.Action] match {
        case yes: JsSuccess[services.Action] => true mustBe true
        case JsError(errors) => false mustBe true
      }
    }
  }
}
