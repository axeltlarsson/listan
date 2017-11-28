package controllers

import javax.inject._

import akka.actor.ActorSystem
import akka.stream.Materializer
import play.api.Logger
import play.api.libs.json.JsValue
import play.api.libs.streams._
import play.api.mvc._
import services.WebSocketActorProvider

import scala.concurrent.Future

@Singleton
class SocketController @Inject()(cc: ControllerComponents)
                                (implicit sys: ActorSystem,
                                 mat: Materializer,
                                 provider: WebSocketActorProvider)
                                 extends AbstractController(cc) {

  def connect = WebSocket.acceptOrResult[JsValue, JsValue] {
    case requestHeader if sameOriginCheck(requestHeader) => {
      val realIp = requestHeader.headers.get("X-Real-IP").getOrElse(requestHeader.remoteAddress)
      Logger.info(s"[$realIp] is ${requestHeader.headers.get("User-Agent").getOrElse("unknown")}")
      Future.successful(Right(ActorFlow.actorRef(out => provider.props(out, ipAddress = realIp))))
    }
    case rejected =>
      Logger.error(s"Request $rejected failed same origin check")
      Future.successful(Left(Forbidden("forbidden")))
  }

  private def sameOriginCheck(requestHeader: RequestHeader): Boolean = {
    requestHeader.headers.get("Origin") match {
      case Some(originValue) if originMatches(originValue) =>
        Logger.debug(s"[${requestHeader.remoteAddress}] originCheck: originValue = $originValue")
        true
      case Some(badOrigin) =>
        /*Logger.error(
          s"originCheck: rejecting request because Origin header value $badOrigin"
          + " is not in the same origin")*/
        true // remember to change to false in production so to speak
      case None =>
        Logger.error(
         s"[${requestHeader.remoteAddress}] originCheck: rejecting request because no Origin header found")
        false
    }
  }

  private def originMatches(origin: String): Boolean = {
    origin.contains("localhost:9000")
  }

}

