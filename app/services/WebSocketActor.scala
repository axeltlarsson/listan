package services

import akka.actor._
import akka.actor.FSM.Event
import play.api.libs.json.{Json, JsValue, JsError, JsSuccess}
import play.Logger
import scala.concurrent.duration._
import scala.concurrent.{Future, Promise}
import scala.util.{Failure, Success}
import java.util.concurrent.TimeoutException
import javax.inject._
import play.api.Configuration
import models.{User, Item}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import akka.pattern.pipe
import akka.pattern.after
import akka.actor.PoisonPill
import scala.collection.mutable
import scala.language.postfixOps

// States in the FSM
sealed trait State
case object Unauthenticated extends State
case object Authenticated extends State

// State data
sealed trait Data
case object NoData extends Data
case class UserData(user: User) extends Data

class WebSocketActor(
  ws: ActorRef,
  userService: UserService,
  listActor: ActorRef,
  ipAddress: String) extends LoggingFSM[State, Data] {

  val ackMap = mutable.Map[String, Promise[Ack]]()

  startWith(Unauthenticated, NoData)
  ws ! Json.toJson(AuthRequest(): Message)

  when(Unauthenticated) {
    case Event(json: JsValue, _) => {
      val ack = (json \ "ack").asOpt[String]
      json.validate[Message] match {
        case s: JsSuccess[Message] => {
          s.get match {
            case Auth(token, ack) => {
              userService.authenticate(token) match {
                case Some(user) => {
                  ws ! Json.toJson(AuthResponse("Authentication success", ack): Message)
                  goto(Authenticated) using UserData(user)
                }
                case None => {
                  ws ! Json.toJson(FailureResponse("Authentication failure", ack): Message)
                  self ! PoisonPill
                  stay
                }
              }
            }
            case _ => {
              val msg = "Invalid message at this state (Unauthenticated)"
              Logger.warn(s"[$ipAddress] $msg")
              ws ! Json.toJson(FailureResponse(msg, ack.getOrElse("NO_ACK_PROVIDED")): Message)
              self ! PoisonPill
              stay
            }
          }
        }
        case e: JsError => {
          val msg = s"Could not validate json ($json) as Message"
          Logger.error(s"[$ipAddress] $msg")
          ws ! Json.toJson(FailureResponse(msg, ack.getOrElse("NO_ACK_PROVIDED")): Message)
          self ! PoisonPill
          stay
        }

      }
    }
  }

  onTransition {
    case Unauthenticated -> Authenticated =>
      stateData match {
        case _ => {
          Logger.info(s"[$ipAddress] authenticated WebSocket")
          listActor ! ListActor.Subscribe
        }
      }
  }

  when(Authenticated) {
    case Event(json: JsValue, _) =>
      val ack = (json \ "ack").asOpt[String]
      json.validate[Message] match {
        case s: JsSuccess[Message] => {
          json.validate[Ack].foreach(a => {
            ackMap.remove(a.ack).foreach(_.success(a))
          })
          Logger.debug(s"[$ipAddress] received $json")
          listActor ! s.get
          stay
        }
        case e: JsError => {
          Logger.error(s"[$ipAddress] Could not validate json as Message")
          ws ! Json.toJson(FailureResponse("Invalid message", ack.getOrElse("NO_ACK_PROVIDED")): Message)
          self ! PoisonPill
          stay
        }
      }
    case Event(r: Response, _) =>  {
      ws ! Json.toJson(r: Message)
      stay
    }
    case Event(a: Action, _) => {
      ws ! Json.toJson(a: Message)

      val ack = Promise[Ack]()
      ackMap += ((Json.toJson(a: Message) \ "ack").as[String] -> ack)
      lazy val ackF = ack.future
      lazy val timeout = after(duration = 5 seconds, using = context.system.scheduler)(
        Future.failed(new TimeoutException("No Ack provided within 1 second")))

      Future firstCompletedOf Seq(ackF, timeout) onComplete {
        case Success(x) => Logger.debug(s"[$ipAddress] ack received within specified time")
        case Failure(e) => {
          Logger.debug(s"[$ipAddress] Committing suicide: e.getMessage")
          self ! PoisonPill
        }
      }
      stay
    }
  }

  override def postStop() = {
    listActor ! ListActor.Unsubscribe
    Logger.info(s"[$ipAddress] closed WebSocket")
  }
}

@Singleton
class WebSocketActorProvider @Inject() (
  userService: UserService,
  @Named("list-actor") listActor: ActorRef,
  ipAddress: String) {

  def props(ws: ActorRef, ipAddress: String) = Props(new WebSocketActor(ws, userService, listActor, ipAddress))
  def get(ws: ActorRef, ipAddress: String) = new WebSocketActor(ws, userService, listActor, ipAddress)
}
