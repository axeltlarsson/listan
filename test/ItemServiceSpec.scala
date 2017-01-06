import org.scalatestplus.play._
import org.scalatest.mock.MockitoSugar
import org.mockito.Mockito._
import org.mockito.Matchers._
import org.scalatest._

import play.api.test._
import play.api.test.Helpers._
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.inject.{bind, Injector}
import scala.concurrent.{Future, Await}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

import models.{Item, ItemRepository, SlickItemRepository}

class ItemServiceSpec extends PlaySpec with MockitoSugar with Inject {

  "SlickItemRepository#add(contents)" should {
    "accept a client-given uuid" in {
      lazy val repo = inject[ItemRepository]
      val clientUuid = "abc-123"
      val uuid = Await.result(repo.add("an item", Option(clientUuid)), 100 millis)
      val allItems = Await.result(repo.all(), 100 millis)
      uuid mustBe clientUuid
      repo.delete(uuid)
    }

    "return uuid and actually insert the item correctly" in {
      lazy val repo = inject[ItemRepository]
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

  "SlickItemRepository#delete(uuid)" should {
    "return nbr of rows affected and actually delete the item" in {
      lazy val repo = inject[ItemRepository]
      val affectedRows = for {
        uuid <- repo.add("to be deleted")
        affectedRows <- repo.delete(uuid)
      } yield affectedRows
      Await.result(affectedRows, 1 seconds) mustBe 1
      val allItems = Await.result(repo.all(), 1 seconds)
      allItems.length mustBe 1 // because earlier test
    }

    "not crash for non-existing uuid" in {
      lazy val repo = inject[ItemRepository]
      Await.result(repo.delete("bogus"), 1 seconds) mustBe 0
    }
  }

  "SlickItemRepository#edit(uuid, contents)" should {
    "properly update contents of item" in {
      lazy val repo = inject[ItemRepository]

      val affectedRows = for {
        uuid <- repo.add("to be updated")
        affectedRows <- repo.edit(uuid, "updated contents")
      } yield affectedRows
      Await.result(affectedRows, 1 seconds) mustBe 1
    }

    "do not crash when trying to edit non-existing item" in {
      lazy val repo = inject[ItemRepository]
      Await.result(repo.edit("bogus", "updated value"), 1 seconds) mustBe 0
    }
  }

  "SlickItemRepository complete and uncomplete" should {
    "work" in {
      lazy val repo = inject[ItemRepository]

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
      lazy val repo = inject[ItemRepository]
      Await.result(repo.complete("bogus"), 1 seconds) mustBe 0
      Await.result(repo.uncomplete("bogus"), 1 seconds) mustBe 0
    }
  }
}
