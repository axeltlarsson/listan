package testhelpers

import models.{ItemList, User, UserRepository}
import play.api.inject.Injector
import play.api.inject.guice.GuiceApplicationBuilder
import services.ItemListService

import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration._
import scala.language.postfixOps

/* Inserts an empty list and provides ec, itemRepo */
trait ListHelper {
  val injector: Injector

  def createList()(implicit ec: ExecutionContext): (ItemList.UUID, User.UUID) = {
    val lstService = injector.instanceOf[ItemListService]
    val uRepo = injector.instanceOf[UserRepository]
    val listUserPair = for {
      userUUID <- uRepo.insert(User.create("name", "password"))
      user <- uRepo.authenticate("name", "password")
      listUUID <- lstService.add("a list", user_uuid = user.get.uuid.get) if user.isDefined
    } yield (listUUID, userUUID)
    Await.result(listUserPair, 1 seconds)
  }
}
