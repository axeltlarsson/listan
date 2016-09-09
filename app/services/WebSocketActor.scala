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

// States in the FSM
sealed trait State
case object Unauthenticated extends State
case object Authenticated extends State

// State data
sealed trait Data
case object NoData extends Data
case class UserData(user: User) extends Data

class WebSocketActor (ws: ActorRef, userService: UserService,
  itemService: ItemService,
  listActor: ActorRef) extends LoggingFSM[State, Data] {

  startWith(Unauthenticated, NoData)
  ws ! Json.toJson(AuthRequest(): Message)
  listActor ! (AuthRequest(): Message)
  
  when(Unauthenticated) {
    case Event(json: JsValue, _) => {
      json.validate[Message] match {
        case s: JsSuccess[Message] => {
          s.get match {
            case Auth(token) => {
              userService.authenticate(token) match {
                case Some(user) => {
                  ws ! Json.toJson(Response(true, "Authentication success"))
                  goto(Authenticated) using UserData(user)
                }
                case None => {
                  ws ! Json.toJson(Response(false, "Authentication failure"))
                  stay // or die?
                }
              }
            }
            case _ => {
              val msg = "Invalid message at this state (Unauthenticated)"
              Logger.warn(msg)
              ws ! Json.toJson(Response(false, msg))
              stay // or die?
            }
          }
        }
        case e: JsError => {
          val msg = s"Could not validate json ($json) as Message"
          Logger.error(msg)
          ws ! Json.toJson(Response(false, msg))
          stay // or die?
        }

      }
    }
  }

  onTransition {
    case Unauthenticated -> Authenticated =>
      stateData match {
        case _ => // nothing to do
      }
  }

  def failureResponse(e: Throwable): JsValue = {
    Logger.error(e.getMessage)
    Json.toJson(Response(false, "An error ocurred, see server logs"))
  }

  when(Authenticated) {
    case Event(json: JsValue, _) =>
      json.validate[Message] match {
        case s: JsSuccess[Message] => {
          s.get match {
            case ADD_ITEM(id, contents) => {
              val uuidFuture: Future[Item.UUID] = itemService.add(contents)
              uuidFuture.map {
                uuid => Json.toJson(Response(true, uuid))
              }.recover {
                case e => failureResponse(e)
              } pipeTo ws
              stay
            }
            case EDIT_ITEM(id, contents) => {
              val rowsFuture: Future[Int] = itemService.edit(id, contents)
              rowsFuture.map { rows =>
                rows match {
                  case 1 => Json.toJson(Response(true, "Edited item"))
                  case 0 => Json.toJson(Response(false, "Could not find item to edit"))
                }
              }.recover {
                case e => failureResponse(e)
              } pipeTo ws
              stay
            }
            case TOGGLE_ITEM(id) => {
              val rowsFuture: Future[Int] = itemService.toggle(id)
              rowsFuture.map { rows =>
                rows match {
                  case 1 => Json.toJson(Response(true, "Toggled item"))
                  case 0 => Json.toJson(Response(false, "Could not find item to toggle"))
                }
              }.recover {
                case e => failureResponse(e)
              } pipeTo ws
              stay
            }

            case DELETE_ITEM(id) => {
              val rowsFuture: Future[Int] = itemService.delete(id)
              rowsFuture.map { rows =>
                rows match {
                  case 1 => Json.toJson(Response(true, "Deleted item"))
                  case 0 => Json.toJson(Response(false, "Could not find item to delete"))
                }
              }.recover {
                case e => failureResponse(e)
              } pipeTo ws
              stay
            }

            case ALL() => {
              val itemsFuture: Future[Seq[Item]] = itemService.all()
              itemsFuture.map {
                items => Json.toJson(items)
              }.recover {
                case e => failureResponse(e)
              } pipeTo ws
              stay
            }

            case _ => {
              Logger.info("Unknown message")
              ws ! Json.toJson(Response(false, "Unknown message"))
              stay
            }
          }
        }
        case e: JsError => {
          Logger.error("Could not validate json as Message")
          ws ! Json.toJson(Response(false, "Invalid message"))
          stay
        }
      }
  }

  override def postStop() = {
    Logger.info("WS closed")
  }
}

@Singleton
class WebSocketActorProvider @Inject() (
  userService: UserService,
  itemService: ItemService,
  @Named("list-actor") listActor: ActorRef) {

  def props(ws: ActorRef) = Props(new WebSocketActor(ws, userService, itemService, listActor))
  def get(ws: ActorRef) = new WebSocketActor(ws, userService, itemService, listActor)
}
