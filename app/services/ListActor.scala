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

  def failureAction(e: Throwable, ack: String, sender: ActorRef): FailedAction = {
    Logger.error(e.getMessage)
    FailedAction(FailureResponse("An error ocurred, see server logs", ack), sender)
  }

  def receive = {
    case Subscribe => {
      Logger.debug("Adding client to set")
      clients += sender
    }

    case action @ AddItem(contents, ack, _) => {
      Logger.debug(s"ListActor: Got ADD_ITEM($ack, $contents)")
      val uuidFuture: Future[Item.UUID] = itemService.add(contents)
      val theSender = sender
      uuidFuture.map {
        uuid => SuccessfulAction(action.copy(uuid = Some(uuid)), UUIDResponse("Added item", uuid, action.ack), theSender)
      }.recover {
        case e => failureAction(e, ack, sender)
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

    // TODO: return the actual id of the edited item
    // because potentially id could be a client-side temp id
    case action @ EditItem(uuid, contents, ack) => {
      val rowsFuture: Future[Int] = itemService.edit(uuid, contents)
      val theSender = sender
      rowsFuture.map { rows =>
        rows match {
          case 1 => SuccessfulAction(action, UUIDResponse("Edited item", uuid, ack), theSender)
          case 0 => FailedAction(FailureResponse("Could not find item to edit", ack), theSender)
        }
      }.recover {
        case e => failureAction(e, ack, theSender)
      } pipeTo self
    }

    /*
     * case action @ TOGGLE_ITEM(id) => {
     *  val rowsFuture: Future[Int] = itemService.toggle(id)
     *  val theSender = sender
     *  rowsFuture.map { rows =>
     *    rows match {
     *      case 1 => SuccessfulAction(action, StatusResponse("Toggled item"), theSender)
     *      case 0 => FailedAction(FailureResponse("Could not find item to toggle"), theSender)
     *    }
     *  }.recover {
     *    case e => failureAction(e, sender)
     *  } pipeTo self
     * }
     */

   // TODO: return id of deleted item instead
   case action @ DeleteItem(uuid, ack) => {
     val rowsFuture: Future[Int] = itemService.delete(uuid)
     val theSender = sender
     rowsFuture.map { rows =>
       rows match {
         case 1 => SuccessfulAction(action, UUIDResponse("Deleted item", uuid, ack), theSender)
         case 0 => FailedAction(FailureResponse("Could not find item to delete", ack), theSender)
       }
     }.recover {
       case e => failureAction(e, ack, theSender)
     } pipeTo self
   }

   /*
    * case action @ ALL() => {
    *   val itemsFuture: Future[Seq[Item]] = itemService.all()
    *   val theSender = sender
    *   itemsFuture.map {
    *     items => SuccessfulAction(action, AllResponse(items), theSender)
    *   }.recover {
    *     case e => failureAction(e, theSender)
    *   } pipeTo self
    * }
    */
  }
}
