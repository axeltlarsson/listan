import org.scalatestplus.play._
import org.scalatest.mockito.MockitoSugar
import org.scalatest._
import org.scalatest.Matchers._

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps
import java.sql.Timestamp

import models.{Item, ItemRepository, User, UserRepository}
import org.scalatestplus.play.guice.{GuiceOneAppPerSuite, GuiceOneAppPerTest}
import play.api.inject.guice.GuiceApplicationBuilder
import services.ItemService
import testhelpers.{EvolutionsHelper, ListHelper}

class ItemServiceSpec extends PlaySpec with MockitoSugar with GuiceOneAppPerSuite with BeforeAndAfter
                      with EvolutionsHelper with ListHelper {
  override val injector = (new GuiceApplicationBuilder()).injector
  implicit val ec: ExecutionContext = injector.instanceOf[ExecutionContext]
  val service = injector.instanceOf[ItemService]
  var listUUID = ""
  var userUUID = ""


  def insertItem(contents: String, uuid: Option[Item.UUID] = None): Future[Item.UUID] = {
    service.add(contents, listUUID, uuid)
  }

  before {
    evolve()
    val listUserPair = createListUser()(ec)
    listUUID = listUserPair._1
    userUUID = listUserPair._2
  }

  after {
    clean()
  }

  "SlickItemRepository#add(contents)" should {
    "accept a client-given uuid" in {
      val clientUUID = Some("abc-123")
      val uuid = Await.result(insertItem("an item", clientUUID), 1 second)
      uuid mustBe clientUUID
    }

    "return uuid and actually insert the item correctly" in {
      val uuid = Await.result(insertItem("some contents"), 1 seconds)

      val item = Await.result(service.get(uuid), 10 millis)
      item mustBe defined
      item.foreach(i => {
        i.contents mustBe "some contents"
        i.completed mustBe false
        i.uuid mustBe uuid
      })
    }
  }

  "SlickItemRepository" should {
    "set created and updated timestamps on add" in {
      // Add item
      val creation = new Timestamp(System.currentTimeMillis())
      val uuid = Await.result(insertItem("an item"), 100 millis)
      val item = Await.result(service.get(uuid), 10 millis)
      item mustBe defined
      item.foreach(i => {
        // Check that created and updated fields are ~equal to creation
        i.created mustBe defined
        i.created.foreach(_.getTime() shouldEqual creation.getTime() +- 20)
        i.updated mustBe defined
        i.updated.foreach(_.getTime() shouldEqual creation.getTime() +- 20)
      })
      // Update item
      val update = new Timestamp(System.currentTimeMillis())
      Await.result(service.edit(uuid, "update"), 100 millis)
      val updatedItem = Await.result(service.get(uuid), 10 millis)
      updatedItem.foreach(i => {
        // Created field should still be ~equal to creation, updated ~equal update
        i.created mustBe defined
        i.created.foreach(_.getTime() shouldEqual creation.getTime() +- 20)
        i.updated mustBe defined
        i.updated.foreach(_.getTime() shouldEqual update.getTime() +- 20)
      })
    }
  }

  "SlickItemRepository#delete(uuid)" should {
    "return nbr of rows affected and actually delete the item" in {
      val rowsUuidPair = Await.result(for {
        uuid <- insertItem("to be deleted")
        affectedRows <- service.delete(uuid)
      } yield (affectedRows, uuid),
        30 millis)
      rowsUuidPair._1 mustBe 1
      val item = Await.result(service.get(rowsUuidPair._2), 30 millis)
      item mustBe None
    }

    "not crash for non-existing uuid" in {
      Await.result(service.delete("bogus"), 1 seconds) mustBe 0
    }
  }

  "SlickItemRepository#edit(uuid, contents)" should {
    "properly update contents of item" in {
      val affectedRows = for {
        uuid <- insertItem("to be updated")
        affectedRows <- service.edit(uuid, "updated contents")
      } yield affectedRows
      Await.result(affectedRows, 1 seconds) mustBe 1
    }

    "do not crash when trying to edit non-existing item" in {
      Await.result(service.edit("bogus", "updated value"), 1 seconds) mustBe 0
    }
  }

  "SlickItemRepository complete and uncomplete" should {
    "work" in {
      val results = Await.result(for {
        uuid1 <- insertItem("Complete me!")
        uuid2 <- insertItem("Do NOT complete me")
        success <- service.complete(uuid1)
      } yield (success, uuid1, uuid2),
        100 millis)
      results._1 mustBe 1
      val item = Await.result(service.get(results._2), 10 millis)
      item mustBe defined
      item.get.completed mustBe true

      val item2 = Await.result(service.get(results._3), 10 millis)
      item2 mustBe defined
      item2.get.completed mustBe false

      Await.result(service.unComplete(item.get.uuid), 1 seconds) mustBe 1
      val itemUnCompleted = Await.result(service.get(item.get.uuid), 10 millis)
      itemUnCompleted mustBe defined
      itemUnCompleted.get.completed mustBe false

      val item22 = Await.result(service.get(item2.get.uuid), 10 millis)
      item22.get.completed mustBe false // (still)
    }

    "not crash for non-existing uuid" in {
      Await.result(service.complete("bogus"), 1 seconds) mustBe 0
      Await.result(service.unComplete("bogus"), 1 seconds) mustBe 0
    }
  }

  "SlickItemRepository all()" should {
    "return items sorted by created timestamp" in {
      val itemsF = for {
        item1 <- insertItem("1")
        item2 <- insertItem("2")
        item3 <- insertItem("3")
        item4 <- insertItem("4")
        compl <- service.complete(item2)
        items <- service.itemsByList(listUUID)
      } yield items
      val items = Await.result(itemsF, 1 second)
      items must have length 4
      items(0).contents mustBe "1"
      items(1).contents mustBe "2"
      items(2).contents mustBe "3"
      items(3).contents mustBe "4"
    }
  }

}
