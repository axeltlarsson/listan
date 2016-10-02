package services

import akka.actor._
import javax.inject._
import play.Logger
import scala.collection.mutable
import models.{User, Item}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import akka.pattern.pipe

object ListActor {
  // Used by WebSocketActor to add itself to list of clients
  case object Subscribe
  // Used by "pipeTo self" to propagate information to all clients
  case class SuccessfulAction(action: Action, response: Response, originalSender: ActorRef)
  case class FailedAction(failureResponse: FailureResponse, originalSender: ActorRef)
}

@Singleton
class ListActor @Inject() (itemService: ItemService)
  extends Actor {
  
  import ListActor._

  val clients: mutable.Set[ActorRef] = mutable.Set[ActorRef]()
  
  def failureAction(e: Throwable, sender: ActorRef): FailedAction = {
    Logger.error(e.getMessage)
    FailedAction(FailureResponse("An error ocurred, see server logs"), sender)
  }

  def receive = {
    case Subscribe => {
      Logger.debug("Adding client to set")
      clients += sender
    }

    case action @ ADD_ITEM(id, contents) => {
      val uuidFuture: Future[Item.UUID] = itemService.add(contents)
      uuidFuture.map {
        uuid => SuccessfulAction(action, UUIDResponse("Added item", uuid), sender)
      }.recover {
        case e => failureAction(e, sender)
      } pipeTo self
    }

    // Propagate successful actions to all but original sender
    case SuccessfulAction(action, response, originalSender) => {
      clients.filter(_ != originalSender).foreach(_ ! action)
      originalSender ! response
    }

    // Send the FailureResponse to the original sender only
    case FailedAction(failureResponse, originalSender) => {
      originalSender ! failureResponse
    }

/*    case EDIT_ITEM(id, contents) => {
      val rowsFuture: Future[Int] = itemService.edit(id, contents)
      rowsFuture.map { rows =>
        rows match {
          case 1 => Response(true, "Edited item")
          case 0 => Response(false, "Could not find item to edit")
        }
      }.recover {
        case e => failureAction(e, sender)
      } pipeTo sender
      
    }
    case TOGGLE_ITEM(id) => {
      val rowsFuture: Future[Int] = itemService.toggle(id)
      rowsFuture.map { rows =>
        rows match {
          case 1 => Response(true, "Toggled item")
          case 0 => Response(false, "Could not find item to toggle")
        }
      }.recover {
        case e => failureAction(m, sender)
      } pipeTo sender
      
    }

    case DELETE_ITEM(id) => {
      val rowsFuture: Future[Int] = itemService.delete(id)
      rowsFuture.map { rows =>
        rows match {
          case 1 => Response(true, "Deleted item")
          case 0 => Response(false, "Could not find item to delete")
        }
      }.recover {
        case e => failureAction(e, sender)
      } pipeTo sender
    }

    case ALL() => {
      val itemsFuture: Future[Seq[Item]] = itemService.all()
      itemsFuture.recover {
        case e => failureAction(e, sender)
      } pipeTo sender
    }*/
  }
}
