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
            case Message(auth, ADD_ITEM(id, contents)) => {
              Logger.info(s"ADD_ITEM($id, $contents)")
              ws ! Json.obj("message" -> "added item")
            }
            case Message(auth, EDIT_ITEM(id, contents)) => {
              Logger.info(s"EDIT_ITEM($id, $contents)")
              ws ! Json.obj("message" -> "edited item")
            }
            case Message(auth, action) => {
              Logger.info("Unknown action", action)
              self ! PoisonPill
            }
          }
        }
        case e: JsError => {
          Logger.error("Could not validate json as Message")
          ws ! Json.obj("error" -> "Invalid message.")
          self ! PoisonPill 
        }
      }
  }

  override def preStart() = {
    Logger.info("WebSocketActor created, sending auth request")
    ws ! Json.obj("message" -> "authenticate yourself or I will kill you!")
  }

  override def postStop() = {
    Logger.info("WS closed")
  }
}

object WebSocketActor {
  def props(ws: ActorRef) = Props(new WebSocketActor(ws))
}

