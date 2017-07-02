package testhelpers

import models.{ItemList, User, UserRepository}
import play.api.Application
import play.api.inject.Injector
import services.ItemListService

import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration._
import scala.language.postfixOps

/* Inserts an empty list and provides ec, itemRepo */
object ListHelper {
  def createList(injector: Injector): ItemList.UUID = {
    val lstService = injector.instanceOf[ItemListService]
    implicit val ec = injector.instanceOf[ExecutionContext]
    val uRepo = injector.instanceOf[UserRepository]
    val listUUIDF = for {
      userUUUID <- uRepo.insert(User.create("name", "password"))
      user <- uRepo.authenticate("name", "password")
      listUUID <- lstService.add("a list", user = user.get) if user.isDefined
    } yield listUUID
    Await.result(listUUIDF, 1 seconds)
  }
}
