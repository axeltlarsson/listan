package testhelpers

import models.{ItemList, User}
import play.api.inject.Injector
import services.{ItemListService, UserService}

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext}
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
    val userService = injector.instanceOf[UserService]
    val listUserPair = for {
      userUUID <- userService.add(userName, "password")
      user <- userService.authenticate(userName, "password")
      listUUID <- lstService.add(s"a list for $userName", userUuid = user.get.uuid)
    } yield (listUUID, userUUID)
    Await.result(listUserPair, 1 second)
  }
}
