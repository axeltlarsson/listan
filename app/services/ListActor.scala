package services

import javax.inject._

import java.lang.IllegalStateException
import akka.actor._
import akka.pattern.pipe
import models.{Item, User}
import play.api.Logger

import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps

object ListActor {
  // Used by WebSocketActor to add itself to list of clients
  case class Subscribe(user: User)
  case object UnSubscribe // used by WebSocketActor to unsubscribe
  // Can be used by "pipeTo self" to propagate information to all clients
  case class SuccessfulAction(action: Action, response: Response, originalSender: ActorRef, user: Option[User] = None)
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

    // Propagate successful actions to all clients of same user, except the original sender
    case SuccessfulAction(action, response, originalSender, userOpt) => {
      // userOpt is passed in by internal clients (AatchAddController) that do not have a WebSocketActor
      // otherwise, the user is looked up from the clients map for external WebSocketActors
      val user = userOpt.getOrElse(clients.get(originalSender).getOrElse {
        logger.error(s"User not found for sender $originalSender. This might indicate a bug.")
        throw new IllegalStateException("User must be defined for external senders")
      })
      // Broadcast to all clients associated with the same user except the original sender
      // make sure to compare user on uuid only - internal user will be fetched from db and will
      // have additional fields set (pasword_hash, created, updated) but external websocket users
      // will only have uuid and name set - taken directly from jwt
      clients.filter { case (client, u) => client != originalSender && u.uuid == user.uuid }
        .keys
        .foreach { client => {
          client ! action
        }
      }

      originalSender ! response
    }

    // Send the FailureResponse to the original sender only
    case FailedAction(failureResponse, originalSender) => {
      originalSender ! failureResponse
    }

    case Subscribe(user) => {
      logger.debug(s"subscribing client sender: ${sender}, user_name: ${user.name} with uuid: ${user.uuid}")
      clients += (sender -> user)
    }

    case UnSubscribe => {
      logger.debug("Unsubscribing client")
      clients -= sender
    }

    case action @ AddItem(contents, lst, ack, clientId, userOpt) => {
      val uuidFuture: Future[Item.UUID] = itemService.add(contents, lst, clientId)
      val theSender = sender
      uuidFuture.map{
        uuid => SuccessfulAction(
          action.copy(uuid = Some(uuid)), UUIDResponse("Added item", uuid, action.ack), theSender, userOpt
        )
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
      val stateFuture = for {
        lists <- if (user.isDefined) itemListService.listsByUser(user.get)
        else Future.failed(throw new Exception("Sender of GetState is not a subscribed client!"))
        items <- Future.traverse(lists)(list => itemService.itemsByList(list.uuid))
      } yield (lists, items.flatten)

      // Pipe directly to theSender since we do not want the response to go out to all subscribed clients
      stateFuture.map{
        state => GetStateResponse(state._1, state._2, ack)
      }.recover{
        case e => {
          logger.error(s"Could not get state response: $e")
          FailureResponse("Could not get state response, check server logs for details", ack)
        }
      } pipeTo theSender
    }

    case action @ AddList(name, description, ack, uuid) => {
      val theSender = sender
      val userUuid = for {
        user <- clients.get(theSender)
      } yield user.uuid

      val uuidFuture = for {
        listUuid <- if (userUuid.isDefined) itemListService.add(name, description, userUuid.get, uuid)
        else Future.failed(throw new Exception("Sender of AddList is not a subscribed client"))
      } yield listUuid

      uuidFuture.map{
        listUuid => SuccessfulAction(action.copy(uuid = Some(listUuid)), UUIDResponse("Added list", listUuid, ack), theSender)
      }.recover{
        case e => failureAction(e, ack, theSender)
      } pipeTo self
    }
    case action @ UpdateListName(uuid, name, ack) => {
      val theSender = sender

      itemListService.updateName(name, uuid).map{
        case true => SuccessfulAction(action, UUIDResponse("Updated list name", uuid, ack), theSender)
        case false => failureAction(throw new Exception("ItemListService did not update name"), ack, theSender)
      }.recover{
        case e => failureAction(e, ack, theSender)
      } pipeTo self
    }

    case action @ UpdateListDescription(uuid, description, ack) => {
      val theSender = sender
      itemListService.updateDescription(description, uuid).map{
        case true => SuccessfulAction(action, UUIDResponse("Updated list description", uuid, ack), theSender)
        case false => failureAction(throw new Exception("ItemListService did not update description"), ack, theSender)
      }.recover {
        case e => failureAction(e, ack, theSender)
      } pipeTo self
    }

    case action @ DeleteList(uuid, ack) => {
      val theSender = sender
      itemListService.delete(uuid).map{
        case true => SuccessfulAction(action, UUIDResponse("Deleted list", uuid, ack), theSender)
        case false => failureAction(throw new Exception("ItemListService did not delete list"), ack, theSender)
      }.recover {
        case e => failureAction(e, ack, theSender)
      } pipeTo self
    }

    case message => {
      logger.error(s"Received unhandled message $message")
    }
  }
}
