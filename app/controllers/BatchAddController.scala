package controllers

import javax.inject._
import play.api.mvc._
import play.api.libs.json.{JsValue, Json}
import services.{JwtValidator, ItemService, ItemListService}
import models.{UserRepository, User}
import play.api.Logging

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class BatchAddController @Inject()(cc: ControllerComponents,
                                   userRepository: UserRepository,
                                   itemListService: ItemListService,
                                   itemService: ItemService)
                                  (implicit ec: ExecutionContext)
    extends AbstractController(cc) with Logging {

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
      case None       => Future.failed(BatchAddError.Unauthorized("User not found"))
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

  private def addItemsToList(itemContents: Seq[String], listUuid: String, user: User): Future[Result] = {
    logger.info(s"Adding ${itemContents.size} items for user ${user.name}")
    val addItemFutures = itemContents.map(itemService.add(_, listUuid))
    Future.sequence(addItemFutures).map { _ =>
      logger.info(s"Successfully added ${itemContents.size} items for user ${user.name}")
      Ok(Json.obj(
        "message" -> s"Batch items added for user ${user.name}",
        "items" -> itemContents
      ))
    }
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
