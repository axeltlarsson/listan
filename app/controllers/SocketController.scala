package controllers

import play.Logger

import play.api.mvc._
import play.api.libs.streams._
import akka.actor.ActorSystem
import akka.stream.Materializer
import javax.inject._
import akka.actor._
import scala.concurrent.Future
import play.api.mvc._
import play.api.libs.streams._
import play.api.libs.json.{JsValue}
import pdi.jwt.{JwtJson, JwtAlgorithm}
import services.WebSocketActorProvider
import play.api.Configuration

@Singleton
class SocketController @Inject() (implicit sys: ActorSystem, mat: Materializer, conf: Configuration, provider: WebSocketActorProvider)
  extends Controller {

  def connect = WebSocket.acceptOrResult[JsValue, JsValue] {
    case requestHeader if sameOriginCheck(requestHeader) => {
      Future.successful(Right(ActorFlow.actorRef(out => provider.props(out))))
    }
    case rejected =>
      Logger.error(s"Request $rejected failed same origin check")
      Future.successful(Left(Forbidden("forbidden")))  
  }

  def sameOriginCheck(requestHeader: RequestHeader): Boolean = {
    requestHeader.headers.get("Origin") match {
      case Some(originValue) if originMatches(originValue) =>
        Logger.debug(s"originCheck: originValue = $originValue")
        true
      case Some(badOrigin) =>
        /*Logger.error(
          s"originCheck: rejecting request because Origin header value $badOrigin"
          + " is not in the same origin")*/
        true // remember to change to false in production so to speak
      case None =>
        Logger.error(
          "originCheck: rejecting request because no Origin header found")
        false
    }
  }

  def originMatches(origin: String): Boolean = {
    origin.contains("localhost:9000")
  }

}

