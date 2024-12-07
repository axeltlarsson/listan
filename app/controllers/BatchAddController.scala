package controllers

import javax.inject._
import play.api.mvc._
import play.api.libs.json.{JsValue, Json}
import services.{JwtValidator, ItemListService, ListActor, AddItem, UUIDResponse}
import models.{UserRepository, User, Item}
import play.api.Logging
import akka.actor.ActorRef
import akka.pattern.ask
import akka.util.Timeout

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class BatchAddController @Inject()(cc: ControllerComponents,
                                   userRepository: UserRepository,
                                   itemListService: ItemListService,
                                   @Named("list-actor") listActor: ActorRef)
                                  (implicit ec: ExecutionContext)
    extends AbstractController(cc) with Logging {

  implicit val timeout: Timeout = 5.seconds // For ask pattern

  def batchAdd: Action[JsValue] = Action.async(parse.json) { request =>
    extractToken(request.headers.get("Authorization"))
      .flatMap(validateToken)
      .flatMap(getUser)
      .flatMap { user =>
        parsePayload(request.body)
          .flatMap { itemContents =>
            findPrimaryList(user)
              .flatMap { primaryList =>
                addItemsToList(itemContents, primaryList.uuid, user)
                  .map { addedItems =>
                    Ok(Json.obj(
                      "message" -> "Batch items added",
                      "items" -> addedItems.map(_.contents),
                      "list_name" -> primaryList.name
                    ))
                  }
              }
          }
      }
      .recover {
        case e: BatchAddError => e.toResult
      }
  }

  private def extractToken(authHeader: Option[String]): Future[String] = {
    authHeader match {
      case Some(header) => Future.successful(header.replace("Bearer ", ""))
      case None         => Future.failed(BatchAddError.Unauthorized("Missing Authorization header"))
    }
  }

  private def validateToken(token: String): Future[String] = {
    JwtValidator.validateToken(token) match {
      case Right(claim) => Future.successful(claim.subject.getOrElse(throw BatchAddError.Unauthorized("Invalid user in token")))
      case Left(error)  => Future.failed(BatchAddError.Unauthorized(error))
    }
  }

  private def getUser(userName: String): Future[User] = {
    userRepository.findByName(userName).flatMap {
      case Some(user) => Future.successful(user)
      case None       => Future.failed(BatchAddError.Unauthorized(s"User $userName not found"))
    }
  }

  private def parsePayload(body: JsValue): Future[Seq[String]] = {
    body.validate[Seq[String]].fold(
      _       => Future.failed(BatchAddError.BadRequest("Invalid payload")),
      success => Future.successful(success)
    )
  }

  private def findPrimaryList(user: User): Future[models.ItemList] = {
    itemListService.listsByUser(user).flatMap {
      _.headOption match {
        case Some(list) => Future.successful(list)
        case None       => Future.failed(BatchAddError.BadRequest("No primary list found for user"))
      }
    }
  }

  private def addItemsToList(itemContents: Seq[String], listUuid: String, user: User): Future[Seq[models.Item]] = {
    Future.sequence(itemContents.map { contents =>
      (listActor ? AddItem(contents, listUuid, ack = "BATCH_ADD", uuid = None, user = Some(user)))
        .mapTo[UUIDResponse]
        .map { response =>
          models.Item(response.uuid, contents, completed = false, listUuid)
        }
    })
  }
}

sealed trait BatchAddError extends Exception {
  def toResult: Result
}

object BatchAddError {
  case class Unauthorized(message: String) extends BatchAddError {
    override def toResult: Result = Results.Unauthorized(Json.obj("error" -> message))
  }
  case class BadRequest(message: String) extends BatchAddError {
    override def toResult: Result = Results.BadRequest(Json.obj("error" -> message))
  }
}
