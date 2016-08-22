package services

import akka.actor._
import akka.actor.FSM.Event
import play.api.libs.json.{Json, JsValue, JsError, JsSuccess}
import play.Logger
import scala.concurrent.duration._

// States in the FSM
sealed trait State
case object Unauthenticated extends State
case object Authenticated extends State

// State data
sealed trait Data
case object Uninitialized extends Data

class WebSocketActor(ws: ActorRef) extends FSM[State, Data] {
  startWith(Unauthenticated, Uninitialized)
  ws ! Json.toJson(AuthRequest(): Message)


// Maybe re-implement websocket flow so that we receive Message:s and not JsValue:s?
  when(Unauthenticated) {
    case Event(msg: Message, _) => {
      msg match {
        case ADD_ITEM => 
        case EDIT_ITEM =>
      }

      }
    }
      json.validate[Message] match {
        case s: JsSucces[Message] => {

        }
        case e: JsError => {
          Logger.error("Could not validate json as Message")
          ws ! Json.toJson(Response(false, "Invalid message"))
          self ! PoisonPill

      }

      case Auth(token) => {
        // check that token is valid
        // if (valid(token))
          goto(Authenticated) using Uninitialized replying (Response(true, "Authenticated"))
      }
    }
  }
  onTransition {
    case Unauthenticated -> Authenticated =>
      stateData match {
        case _ => // nothing to do
      }
  }

  when(Authenticated) {
    case Event(json: JsValue, _) =>
      json.validate[Message] match {
        case s: JsSuccess[Message] => {
          s.get match {
            case ADD_ITEM(id, contents) => {
              Logger.info(s"ADD_ITEM($id, $contents)")
              ws ! Json.toJson(Response(true, "added item"))
            }
            case EDIT_ITEM(id, contents) => {
              Logger.info(s"EDIT_ITEM($id, $contents)")
              ws ! Json.toJson(Response(true, "edited item"))
            }
            case _ => {
              Logger.info("Unknown message")
              ws ! Json.toJson(Response(false, "Unknown message"))
              self ! PoisonPill
            }
          }
        }
        case e: JsError => {
          Logger.error("Could not validate json as Message")
          ws ! Json.toJson(Response(false, "Invalid message"))
          self ! PoisonPill 
        }
      }
  }


  override def preStart() = {
    Logger.info("WebSocketActor created, sending auth request")
    ws ! Json.toJson(AuthRequest(): Message)
  }

  override def postStop() = {
    Logger.info("WS closed")
  }
}

object WebSocketActor {
  def props(ws: ActorRef) = Props(new WebSocketActor(ws))
}

