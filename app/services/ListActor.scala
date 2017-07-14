package services

import javax.inject._

import akka.actor._
import akka.pattern.pipe
import models.{Item, ItemList, User, UserRepository}
import play.api.Logger

import scala.collection.mutable
import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.language.postfixOps
import scala.util.{Failure, Success}

object ListActor {
  // Used by WebSocketActor to add itself to list of clients
  case class Subscribe(user: User)
  case object UnSubscribe // used by WebSocketActor to unsubscribe
  // Can be used by "pipeTo self" to propagate information to all clients
  case class SuccessfulAction(action: Action, response: Response, originalSender: ActorRef)
  case class FailedAction(failureResponse: FailureResponse, originalSender: ActorRef)
}

@Singleton
class ListActor @Inject()(itemService: ItemService, itemListService: ItemListService)
                         (implicit ec: ExecutionContext)
                          extends Actor {
  import ListActor._

  val logger = Logger(this.getClass.getName)
  val clients: mutable.Map[ActorRef, User] = mutable.Map[ActorRef, User]()

  def failureAction(e: Throwable, ack: String, sender: ActorRef): FailedAction = {
    logger.error(e.getMessage)
    FailedAction(FailureResponse("An error ocurred, see server logs", ack), sender)
  }

  def receive = {

    // Propagate successful actions to all but original sender
    case SuccessfulAction(action, response, originalSender) => {
      clients.keys.filter(_ != originalSender).foreach(_ ! action)
      originalSender ! response
    }

    // Send the FailureResponse to the original sender only
    case FailedAction(failureResponse, originalSender) => {
      originalSender ! failureResponse
    }

    case Subscribe(user) => {
      logger.debug(s"subscribing client ${user.uuid}")
     clients += (sender -> user)
    }

    case UnSubscribe => {
      logger.debug("Unsubscribing client")
      clients -= sender
    }

    case action @ AddItem(contents, lst, ack, clientId) => {
      val uuidFuture: Future[Item.UUID] = itemService.add(contents, lst, clientId)
      val theSender = sender
      uuidFuture.map{
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
      rowsFuture.map{
        case 1 => SuccessfulAction(action, UUIDResponse("Completed item", uuid, ack), theSender)
        case _ => FailedAction(FailureResponse("Could not find item to complete", ack), theSender)
      }.recover {
        case e => failureAction(e, ack, theSender)
      } pipeTo self
    }

    case action @ UnCompleteItem(uuid, ack) => {
      val rowsFuture: Future[Int] = itemService.unComplete(uuid)
      val theSender = sender
      rowsFuture.map{
        case 1 => SuccessfulAction(action, UUIDResponse("Uncompleted item", uuid, ack), theSender)
        case _ => FailedAction(FailureResponse("Could not find item to uncomplete", ack), theSender)
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

   case GetState(ack) => {
     val theSender = sender
     val user = clients.get(sender)
     val listFuture = for {
       lists <- if (user.isDefined) itemListService.itemListsByUser(user.get)
                else Future.failed(throw new Exception("Sender of GetState is not a subscribed client!"))
     } yield lists

     // Pipe directly to theSender since we do not want the response to go out to all subscribed clients
     listFuture.map{
       lists => GetStateResponse(lists, ack)
     }.recover{
       case e => {
         logger.error(s"Could not get state reponse: $e")
         FailureResponse("Could not get state response, check server logs for details", ack)
       }
     } pipeTo theSender
   }

    case action @ AddList(name, description, ack, uuid) => {
      val theSender = sender
      val user_uuid = for {
        user <- clients.get(theSender)
        user_uuid <- user.uuid
      } yield user_uuid

      val uuidFuture = for {
        listUuid <- if (user_uuid.isDefined) itemListService.add(name, description, user_uuid.get, uuid)
                 else Future.failed(throw new Exception("Sender of AddList is not a subscribed client"))
      } yield listUuid

      uuidFuture.map{
        listUuid => SuccessfulAction(action, UUIDResponse("Added list", listUuid, ack), theSender)
      }.recover{
        case e => failureAction(e, ack, theSender)
      } pipeTo self
    }

    case message => {
      logger.error(s"Received unhandled message $message")
    }
  }
}
