package services

import akka.actor._
import javax.inject._

import play.Logger

import scala.collection.mutable
import models.{Item, ItemList, User}

import scala.concurrent.{ExecutionContext, Future}
import akka.pattern.pipe

import scala.util.{Failure, Success}

object ListActor {
  // Used by WebSocketActor to add itself to list of clients
  case object Subscribe
  case object Unsubscribe // used by WebSocketActor to unsubscribe
  // Used by "pipeTo self" to propagate information to all clients
  case class SuccessfulAction(action: Action, response: Response, originalSender: ActorRef)
  case class FailedAction(failureResponse: FailureResponse, originalSender: ActorRef)
}

@Singleton
class ListActor @Inject()(itemService: ItemService, lstService: ItemListService)
                         (implicit ec: ExecutionContext)
                          extends Actor {
  import ListActor._

  val clients: mutable.Set[ActorRef] = mutable.Set[ActorRef]()

  def failureAction(e: Throwable, ack: String, sender: ActorRef): FailedAction = {
    Logger.error(e.getMessage)
    FailedAction(FailureResponse("An error ocurred, see server logs", ack), sender)
  }

  def receive = {

    // Propagate successful actions to all but original sender
    case SuccessfulAction(action, response, originalSender) => {
      clients.filter(_ != originalSender).foreach(_ ! action)
      originalSender ! response
    }

    // Send the FailureResponse to the original sender only
    case FailedAction(failureResponse, originalSender) => {
      originalSender ! failureResponse
    }

    case Subscribe => {
      Logger.debug("Subscribing client")
      clients += sender
    }

    case Unsubscribe => {
      Logger.debug("Unsubscribing client")
      clients -= sender
    }

    case action @ AddItem(contents, lst, ack, clientId) => {
      val uuidFuture: Future[String] = itemService.add(contents, lst, clientId)
      val theSender = sender
      uuidFuture.map {
        uuid => SuccessfulAction(action.copy(uuid = Some(uuid)), UUIDResponse("Added item", uuid, action.ack), theSender)
      }.recover {
        case e => failureAction(e, ack, sender)
      } pipeTo self
    }


    case action @ EditItem(uuid, contents, ack) => {
      val rowsFuture: Future[Int] = itemService.edit(uuid, contents)
      val theSender = sender
      rowsFuture.map{
        case 1 => SuccessfulAction(action, UUIDResponse("Edited item", uuid, ack), theSender)
        case 0 => FailedAction(FailureResponse("Could not find item to edit", ack), theSender)
      }.recover {
        case e => failureAction(e, ack, theSender)
      } pipeTo self
    }

    case action @ CompleteItem(uuid, ack) => {
      val rowsFuture: Future[Int] = itemService.complete(uuid)
      val theSender = sender
      rowsFuture.map { rows =>
        if (rows == 1)
          SuccessfulAction(action, UUIDResponse("Completed item", uuid, ack), theSender)
        else
          FailedAction(FailureResponse("Could not find item to complete", ack), theSender)
      }.recover {
        case e => failureAction(e, ack, theSender)
      } pipeTo self
    }

    case action @ UncompleteItem(uuid, ack) => {
      val rowsFuture: Future[Int] = itemService.unComplete(uuid)
      val theSender = sender
      rowsFuture.map { rows =>
        if (rows == 1)
          SuccessfulAction(action, UUIDResponse("Uncompleted item", uuid, ack), theSender)
        else
          FailedAction(FailureResponse("Could not find item to uncomplete", ack), theSender)
      }.recover {
        case e => failureAction(e, ack, theSender)
      } pipeTo self
    }

   case action @ DeleteItem(uuid, ack) => {
     val rowsFuture: Future[Int] = itemService.delete(uuid)
     val theSender = sender
     rowsFuture.map{
       case 1 => SuccessfulAction(action, UUIDResponse("Deleted item", uuid, ack), theSender)
       case 0 => FailedAction(FailureResponse("Could not find item to delete", ack), theSender)
     }.recover {
       case e => failureAction(e, ack, theSender)
     } pipeTo self
   }

   case action @ GetState(ack) => {
     val itemsFuture: Future[Seq[ItemList]] = lstService.all()
     val theSender = sender
     itemsFuture onComplete {
       case Success(items) => theSender ! GetStateResponse(???, ack)
       case Failure(t) => theSender ! FailureResponse(t.getMessage, ack)
     }
   }
  }
}
