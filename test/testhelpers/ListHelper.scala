package testhelpers

import models.{ItemList, User, UserRepository}
import play.api.inject.Injector
import play.api.inject.guice.GuiceApplicationBuilder
import services.ItemListService

import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration._
import scala.language.postfixOps

trait ListHelper {
  val injector: Injector

  /**
    * Inserts a new user with name `userName` and a new list belonging to said user
    * @param userName - the user name of the new user
    * @return a pair of (the list's UUID, the user's UUID)
    */
  def createListUser(userName: String = "name")(implicit ec: ExecutionContext): (ItemList.UUID, User.UUID) = {
    val lstService = injector.instanceOf[ItemListService]
    val uRepo = injector.instanceOf[UserRepository]
    val listUserPair = for {
      userUUID <- uRepo.insert(User.create(userName, "password"))
      user <- uRepo.authenticate(userName, "password")
      listUUID <- lstService.add(s"a list for $userName", user_uuid = user.get.uuid.get)
    } yield (listUUID, userUUID)
    Await.result(listUserPair, 1 second)
  }
}
