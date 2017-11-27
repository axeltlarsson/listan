import org.scalatest.Matchers._
import java.sql.Timestamp

import models.{ItemList, User}
import services.UserService
import org.scalatest.BeforeAndAfter
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.inject.guice.GuiceApplicationBuilder
import services.{ItemListService, ItemService}

import scala.language.postfixOps
import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration._

import play.api.db.DBApi
import play.api.db.evolutions.Evolutions

class ItemListServiceSpec extends PlaySpec with GuiceOneAppPerSuite with BeforeAndAfter {
  val injector = app.injector
  implicit val ec: ExecutionContext = injector.instanceOf[ExecutionContext]
  val service = injector.instanceOf[ItemListService]
  val dbApi = injector.instanceOf[DBApi]

  before {
    Evolutions.applyEvolutions(dbApi.database("default"))
    val userService = injector.instanceOf[UserService]
    // Insert users and await completion
    val uuidFutures = for {
      uuid1 <- userService.add("user1", "password1")
      uuid2 <- userService.add("user2", "password2")
    } yield (uuid1, uuid2)
    Await.result(uuidFutures, 300 millis)
 }

  after {
    Evolutions.cleanupEvolutions(dbApi.database("default"))
  }

  trait Users {
    // Get the users
    private val userService = injector.instanceOf[UserService]
    private val usersFuture = for {
      user1 <- userService.authenticate("user1", "password1")
      user2 <- userService.authenticate("user2", "password2")
    } yield (user1, user2)
    val users = Await.result(usersFuture, 300 millis)
  }

  "SlickItemListRepository" should {
    "handle creation, updates and deletion of a list" in new Users {
      val creationTime = new Timestamp(System.currentTimeMillis())
      val uuid1 = Await.result(service.add(name = "list1", userUuid = users._1.get.uuid), 30 millis)
      uuid1.length must be > 20
      val list1: Option[ItemList] = Await.result(service.get(uuid1), 30 millis)
      list1 mustBe defined
      list1.foreach(l => {
        l.created mustBe defined
        l.created.foreach(_.getTime() shouldEqual creationTime.getTime() +- 20)
        l.updated mustBe defined
        l.updated.foreach(_.getTime() shouldEqual creationTime.getTime() +- 20)
        l.description mustBe None
        l.name mustBe "list1"
        l.userUuid mustBe users._1.get.uuid
      })


      // Update description
      val updateTime = new Timestamp(System.currentTimeMillis())
      val isSuccessful = Await.result(service.updateDescription(description = "new descr", uuid = uuid1), 30 millis)
      isSuccessful mustBe true
      val list1Again: Option[ItemList] = Await.result(service.get(uuid1), 30 millis)
      list1Again mustBe defined
      list1Again.foreach(l => {
        l.description mustBe Some("new descr")
        l.name mustBe "list1"
        l.created mustBe defined
        l.created.foreach(_.getTime() shouldEqual creationTime.getTime() +- 20)
        l.updated mustBe defined
        l.updated.foreach(_.getTime() shouldEqual updateTime.getTime() +- 20)
        l.userUuid mustBe users._1.get.uuid
      })

      // Update name
      val successfulNameUpdate = Await.result(service.updateName("new name", uuid1), 30 millis)
      successfulNameUpdate mustBe true
      val list1AgainAgain: Option[ItemList] = Await.result(service.get(uuid1), 30 millis)
      list1AgainAgain mustBe defined
      list1AgainAgain.foreach(_.name mustBe "new name")

      // Delete
      val successfulDeletion = Await.result(service.delete(uuid1), 30 millis)
      successfulDeletion mustBe true
      val list1AgainAgainAgain = Await.result(service.get(uuid1), 30 millis)
      list1AgainAgainAgain mustBe None
    }

    "not crash when trying to delete non-existent list" in {
      val successfulDeletion = Await.result(service.delete("i-do-not-exist"), 30 millis)
      successfulDeletion mustBe false
    }

    "delete items belonging to the list that gets deleted" in new Users {
      val itemService = injector.instanceOf[ItemService]
      // create two lists
      val uuid1 = Await.result(service.add(name = "list 1", userUuid = users._1.get.uuid), 30 millis)
      val uuid2 = Await.result(service.add(name = "list 2", userUuid = users._2.get.uuid), 30 millis)
      // Add some items to list1
      val list1ItemIds = Await.result(for {
        item1 <- itemService.add("item 1", uuid1)
        item2 <- itemService.add("item 2", uuid1)
      } yield (item1, item2),
        30 millis)

      // Add some items to list2
      val list2ItemIds = Await.result(for {
        item3 <- itemService.add("item 3 list2", uuid2)
        item4 <- itemService.add("item 4 list2", uuid2)
      } yield (item3, item4),
        30 millis)

      // Get by list and check that they are there
      /* List 1*/
      val itemsList1 = Await.result(itemService.itemsByList(uuid1), 30 millis)
      itemsList1 must have length(2)
      itemsList1(0).uuid shouldEqual list1ItemIds._1
      itemsList1(1).uuid shouldEqual list1ItemIds._2
      // Get by id and check
      val item1 = Await.result(itemService.get(list1ItemIds._1), 30 millis)
      item1 mustBe defined
      val item2 = Await.result(itemService.get(list1ItemIds._2), 30 millis)
      item2 mustBe defined

      /* List 2*/
      val itemsList2 = Await.result(itemService.itemsByList(uuid2), 30 millis)
      itemsList2 must have length(2)
      itemsList2(0).uuid shouldEqual list2ItemIds._1
      itemsList2(1).uuid shouldEqual list2ItemIds._2
      // Get by id and check
      val item3 = Await.result(itemService.get(list1ItemIds._1), 30 millis)
      item3 mustBe defined
      val item4 = Await.result(itemService.get(list1ItemIds._2), 30 millis)
      item4 mustBe defined

      // Delete list1
      val succesfulDelete = Await.result(service.delete(uuid1), 30 millis)
      succesfulDelete mustBe true
      val list1Items = Await.result(itemService.itemsByList(uuid1), 30 millis)
      list1Items must have length(0)
      // Delete list2

      val succesfulDelete2 = Await.result(service.delete(uuid2), 30 millis)
      succesfulDelete2 mustBe true
      val list2Items = Await.result(itemService.itemsByList(uuid2), 30 millis)
      list2Items must have length(0)

    }

    "return lists by user properly" in new Users {
      val uuid1 = Await.result(service.add(name = "list1 by user 1", userUuid = users._1.get.uuid), 30 millis)
      val uuid2 = Await.result(service.add(name = "list2 by user 2", userUuid = users._2.get.uuid), 30 millis)
      val uuid3 = Await.result(service.add(name = "list3 also by user 1", userUuid = users._1.get.uuid), 30 millis)

      val user1Lists = Await.result(service.listsByUser(users._1.get), 30 millis)
      user1Lists must have length(2)
      val user2Lists = Await.result(service.listsByUser(users._2.get), 30 millis)
      user2Lists must have length(1)
    }

    "return ItemList:s for users with their respective Item:s from itemListByUser" in new Users {
      val itemService = injector.instanceOf[ItemService]
      // Add lists
      val uuid1 = Await.result(service.add(name = "list1 by user 1", userUuid = users._1.get.uuid), 30 millis)
      val uuid2 = Await.result(service.add(name = "list2 by user 2", userUuid = users._2.get.uuid), 30 millis)
      val uuid3 = Await.result(service.add(name = "list3 also by user 1", userUuid = users._1.get.uuid), 30 millis)

      // Add items to lists
      val items = Await.result(for {
        item1 <- itemService.add("item 1 list 1", uuid1)
        item2 <- itemService.add("item 2 list 2", uuid2)
        item3 <- itemService.add("item 3 list 3", uuid3)
        item4 <- itemService.add("item 4 list 1", uuid1)
        item5 <- itemService.add("item 5 list 1", uuid1)
      } yield (item1, item2, item3, item4, item5),
        30 millis)

      val user1Lists = Await.result(service.itemListsByUser(users._1.get), 30 millis)
      user1Lists must have length(2)
      user1Lists(0)._1.name mustBe "list1 by user 1"
      user1Lists(0)._2 must have length 3
      user1Lists(0)._2(2).contents mustBe "item 5 list 1"
      user1Lists(1)._1.name mustBe "list3 also by user 1"
      user1Lists(1)._2 must have length 1

      val user2Lists = Await.result(service.itemListsByUser(users._2.get), 30 millis)
      user2Lists must have length 1
      user2Lists(0)._1.name mustBe "list2 by user 2"
      user2Lists(0)._2 must have length 1

    }
  }
}

