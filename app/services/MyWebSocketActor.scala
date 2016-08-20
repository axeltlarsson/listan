package services

import akka.actor._
import play.api.libs.json.JsValue

object MyWebSocketActor {
  def props(out: ActorRef) = Props(new MyWebSocketActor(out))
}

class MyWebSocketActor(out: ActorRef) extends Actor {
  def receive = {
    case msg: JsValue =>
      out ! msg
  }
}