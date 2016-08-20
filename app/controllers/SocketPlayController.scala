package controllers

import play.api.mvc._
import play.api.libs.streams._
import akka.actor.ActorSystem
import akka.stream.Materializer
import javax.inject._
import services.MyWebSocketActor
import play.api.libs.json.JsValue
import scala.concurrent.Future

class SocketPlay @Inject()(implicit system: ActorSystem, materializer: Materializer) extends Controller {

  def socket = WebSocket.accept[JsValue, JsValue] { request =>
     ActorFlow.actorRef(out => MyWebSocketActor.props(out))
  }

  
}
