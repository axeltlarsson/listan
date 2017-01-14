package services

import akka.actor._
import akka.actor.FSM.Event
import play.api.libs.json.{Json, JsValue, JsError, JsSuccess}
import play.Logger
import scala.concurrent.duration._
import scala.concurrent.Future
import javax.inject._
import scala.util.{Success}
import play.api.Configuration
import models.{User, Item}
import scala.concurrent.ExecutionContext.Implicits.global
import akka.pattern.pipe
import akka.actor.PoisonPill

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
  listActor: ActorRef) extends LoggingFSM[State, Data] {

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
              Logger.warn(msg)
              ws ! Json.toJson(FailureResponse(msg, ack.getOrElse("NO_ACK_PROVIDED")): Message)
              self ! PoisonPill
              stay
            }
          }
        }
        case e: JsError => {
          val msg = s"Could not validate json ($json) as Message"
          Logger.error(msg)
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
          Logger.debug("Subscribing to listActor")
          listActor ! ListActor.Subscribe
        }
      }
  }

  when(Authenticated) {
    case Event(json: JsValue, _) =>
      val ack = (json \ "ack").asOpt[String]
      json.validate[Message] match {
        case s: JsSuccess[Message] => {
          Logger.debug("WsActor got msg, sending it further up the chain to ListActor")
          listActor ! s.get
          stay
        }
        case e: JsError => {
          Logger.error("Could not validate json as Message")
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
      stay
    }
  }

  override def postStop() = {
    listActor ! ListActor.Unsubscribe
    Logger.info("WS closed")
  }
}

@Singleton
class WebSocketActorProvider @Inject() (
  userService: UserService,
  @Named("list-actor") listActor: ActorRef) {

  def props(ws: ActorRef) = Props(new WebSocketActor(ws, userService, listActor))
  def get(ws: ActorRef) = new WebSocketActor(ws, userService, listActor)
}
