import org.scalatestplus.play._
import org.scalatest.mockito.MockitoSugar
import org.scalatest._
import org.scalatest.Matchers._

import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps
import java.sql.Timestamp

import models.{ItemRepository, User, UserRepository}
import org.scalatestplus.play.guice.{GuiceOneAppPerSuite, GuiceOneAppPerTest}
import play.api.inject.guice.GuiceApplicationBuilder
import testhelpers.{EvolutionsHelper, ListHelper}

class ItemServiceSpec extends PlaySpec with MockitoSugar with GuiceOneAppPerSuite with BeforeAndAfter with EvolutionsHelper with ListHelper {
  override val injector = (new GuiceApplicationBuilder()).injector
  implicit val ec: ExecutionContext = injector.instanceOf[ExecutionContext]
  val repo = injector.instanceOf[ItemRepository]
  var listUUID = ""

  before {
    evolve()
    listUUID = createList()(ec)
  }

  after {
    clean()
  }

  "SlickItemRepository#add(contents)" should {
    "accept a client-given uuid" in {
      val clientUuid = "abc-123"
      val uuid = Await.result(repo.add("an item", listUUID = listUUID, id = Option(clientUuid)), 100 millis)
      val allItems = Await.result(repo.all(), 100 millis)
      uuid mustBe clientUuid
    }

    "return uuid and actually insert the item correctly" in {
      val allItems0 = Await.result(repo.all(), 1 seconds)
      allItems0.length mustBe 0
      val uuid = Await.result(repo.add("some contents", listUUID = listUUID), 1 seconds)
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
      val uuid = Await.result(repo.add("item", listUUID = listUUID), 100 millis)
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
        uuid <- repo.add("to be deleted", listUUID = listUUID)
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
        uuid <- repo.add("to be updated", listUUID = listUUID)
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
        uuid <- repo.add("Complete me!", listUUID = listUUID)
        uuid2 <- repo.add("Do NOT complete me", listUUID = listUUID)
        affectedRows <- repo.complete(uuid)
      } yield affectedRows
      Await.result(affectedRows, 1 seconds) mustBe 1
      val item = Await.result(repo.all(), 1 seconds)
        .filter(_.contents == "Complete me!").head
      item.completed mustBe true
      val item2 = Await.result(repo.all(), 1 seconds)
        .filter(_.contents == "Do NOT complete me").head
      item2.completed mustBe false

      Await.result(repo.unComplete(item.uuid.get), 1 seconds) mustBe 1
      val itemUncompleted = Await.result(repo.all(), 1 seconds)
        .filter(_.contents == "Complete me!").head
      itemUncompleted.completed mustBe false
      val item22 = Await.result(repo.all(), 1 seconds)
        .filter(_.contents == "Do NOT complete me").head
      item22.completed mustBe false // (still)
    }

    "not crash for non-existing uuid" in {
      Await.result(repo.complete("bogus"), 1 seconds) mustBe 0
      Await.result(repo.unComplete("bogus"), 1 seconds) mustBe 0
    }
  }

  "SlickItemRepository all()" should {
    "return items sorted by created timestamp" in {
      val itemsF = for {
        item1 <- repo.add("1", listUUID = listUUID)
        item2 <- repo.add("2", listUUID = listUUID)
        item3 <- repo.add("3", listUUID = listUUID)
        item4 <- repo.add("4", listUUID = listUUID)
        compl <- repo.complete(item2)
        items <- repo.all()
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
