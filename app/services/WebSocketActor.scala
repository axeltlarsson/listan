package services

import akka.actor._
import play.api.libs.json.{Json, JsValue, JsError, JsSuccess}
import play.Logger

class WebSocketActor(ws: ActorRef) extends Actor {
  var authenticated = false
  def receive = {
    case json: JsValue =>
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
    ws ! Json.toJson(AuthRequest())
  }

  override def postStop() = {
    Logger.info("WS closed")
  }
}

object WebSocketActor {
  def props(ws: ActorRef) = Props(new WebSocketActor(ws))
}

