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
  // move Subscribe to Message?
  case object Subscribe // asking ListActor to add sender to list of clients
}

@Singleton
class ListActor @Inject() (itemService: ItemService)
  extends Actor {
  
  import ListActor._

  val clients: mutable.Set[ActorRef] = mutable.Set[ActorRef]()
  
  def failureResponse(e: Throwable): Response = {
    Logger.error(e.getMessage)
    Response(false, "An error ocurred, see server logs")
  }

  def receive = {
    case Subscribe => {
      Logger.debug("Adding client to set")
      clients += sender
    }

    case ADD_ITEM(id, contents) => {
      val uuidFuture: Future[Item.UUID] = itemService.add(contents)
      uuidFuture.map {
        uuid => Response(true, uuid)
      }.recover {
        case e => failureResponse(e)
      } pipeTo sender
      
    }
    case EDIT_ITEM(id, contents) => {
      val rowsFuture: Future[Int] = itemService.edit(id, contents)
      rowsFuture.map { rows =>
        rows match {
          case 1 => Response(true, "Edited item")
          case 0 => Response(false, "Could not find item to edit")
        }
      }.recover {
        case e => failureResponse(e)
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
        case e => failureResponse(e)
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
        case e => failureResponse(e)
      } pipeTo sender
    }

    case ALL() => {
      val itemsFuture: Future[Seq[Item]] = itemService.all()
      itemsFuture.recover {
        case e => failureResponse(e)
      } pipeTo sender
    }
  }
}
