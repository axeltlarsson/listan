import org.scalatestplus.play._
import org.scalatest.mock.MockitoSugar
import org.mockito.Mockito._
import org.mockito.Matchers._
import org.scalatest._
import org.scalatest.Matchers._

import play.api.test._
import play.api.test.Helpers._
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.inject.{bind, Injector}
import scala.concurrent.{Future, Await}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps
import java.sql.Timestamp

import models.{Item, ItemRepository, SlickItemRepository}

class ItemServiceSpec extends PlaySpec with MockitoSugar with Inject with BeforeAndAfter {
  lazy val repo = inject[ItemRepository]

  after {
    /* Delete all items in repo */
    val items = Await.result(repo.all(), 100 millis)
    items.foreach(i => Await.result(repo.delete(i.uuid.get), 20 millis))
  }

  "SlickItemRepository#add(contents)" should {
    "accept a client-given uuid" in {
      val clientUuid = "abc-123"
      val uuid = Await.result(repo.add("an item", Option(clientUuid)), 100 millis)
      val allItems = Await.result(repo.all(), 100 millis)
      uuid mustBe clientUuid
    }

    "return uuid and actually insert the item correctly" in {
      val allItems0 = Await.result(repo.all(), 1 seconds)
      allItems0.length mustBe 0 // this currently fails when testing all tests, but ok for testOnly
      val uuid = Await.result(repo.add("some contents"), 1 seconds)
      uuid.length must be > 20

      val allItems = Await.result(repo.all(), 1 seconds)
      allItems(0).contents mustBe "some contents"
      allItems(0).completed mustBe false
      allItems(0).uuid.get mustBe uuid
    }
  }

  "SlickItemRepository" should {
    "set set created and updated timestamps on add" in {
      // Add item
      val creation = new Timestamp(System.currentTimeMillis())
      val uuid = Await.result(repo.add("item"), 100 millis)
      val item = Await.result(repo.all(), 100 millis).find(_.uuid.exists(_ == uuid))
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
      Await.result(repo.edit(uuid, "update"), 100 millis)
      val updatedItem = Await.result(repo.all(), 100 millis).find(_.uuid.exists(_ == uuid))
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
      val affectedRows = for {
        uuid <- repo.add("to be deleted")
        affectedRows <- repo.delete(uuid)
      } yield affectedRows
      Await.result(affectedRows, 1 seconds) mustBe 1
      val allItems = Await.result(repo.all(), 1 seconds)
      allItems.length mustBe 0
    }

    "not crash for non-existing uuid" in {
      Await.result(repo.delete("bogus"), 1 seconds) mustBe 0
    }
  }

  "SlickItemRepository#edit(uuid, contents)" should {
    "properly update contents of item" in {
      val affectedRows = for {
        uuid <- repo.add("to be updated")
        affectedRows <- repo.edit(uuid, "updated contents")
      } yield affectedRows
      Await.result(affectedRows, 1 seconds) mustBe 1
    }

    "do not crash when trying to edit non-existing item" in {
      Await.result(repo.edit("bogus", "updated value"), 1 seconds) mustBe 0
    }
  }

  "SlickItemRepository complete and uncomplete" should {
    "work" in {
      val affectedRows = for {
        uuid <- repo.add("Complete me!")
        uuid2 <- repo.add("Do NOT complete me")
        affectedRows <- repo.complete(uuid)
      } yield affectedRows
      Await.result(affectedRows, 1 seconds) mustBe 1
      val item = Await.result(repo.all(), 1 seconds)
        .filter(_.contents == "Complete me!").head
      item.completed mustBe true
      val item2 = Await.result(repo.all(), 1 seconds)
        .filter(_.contents == "Do NOT complete me").head
      item2.completed mustBe false

      Await.result(repo.uncomplete(item.uuid.get), 1 seconds) mustBe 1
      val itemUncompleted = Await.result(repo.all(), 1 seconds)
        .filter(_.contents == "Complete me!").head
      itemUncompleted.completed mustBe false
      val item22 = Await.result(repo.all(), 1 seconds)
        .filter(_.contents == "Do NOT complete me").head
      item22.completed mustBe false // (still)
    }

    "not crash for non-existing uuid" in  {
      Await.result(repo.complete("bogus"), 1 seconds) mustBe 0
      Await.result(repo.uncomplete("bogus"), 1 seconds) mustBe 0
    }
  }
}
