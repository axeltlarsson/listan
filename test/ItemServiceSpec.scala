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

class ItemServiceSpec extends PlaySpec with MockitoSugar {

  // trait MockItemRepo {
  //   val mockItemRep = mock[ItemRepository]
  //   when(mockItemRep.authenticate("axel", "password")) thenReturn Future{Some(User("axel"))}
  //   val app = new GuiceApplicationBuilder()
  //     .overrides(bind[ItemRepository].toInstance(mockItemRep))
  //     .build
  //   val userService = app.injector.instanceOf[UserService]
  // }

  trait Inject {
    import scala.reflect.ClassTag
    lazy val injector = (new GuiceApplicationBuilder).injector
    def inject[T: ClassTag]: T = injector.instanceOf[T]
  }

  "SlickItemRepository#add(contents)" should {
    "return uuid and actually insert the item correctly" in new Inject {
      // val app = new GuiceApplicationBuilder().build
      // val repo = app.injector.instanceOf[ItemRepository]
      lazy val repo = inject[ItemRepository]
      val uuid = Await.result(repo.add("some contents"), 1 seconds)
      uuid.length must be > 20

      val allItems = Await.result(repo.all(), 1 seconds)
      allItems(0).contents mustBe "some contents"
      allItems(0).completed mustBe false
      allItems(0).uuid.get mustBe uuid
    }
  }

  "SlickItemRepository#delete(uuid)" should {
    "return nbr of rows affected and actually delete the item" in new Inject {
      lazy val repo = inject[ItemRepository]
      val affectedRows = for {
        uuid <- repo.add("to be deleted")
        affectedRows <- repo.delete(uuid)
      } yield affectedRows
      Await.result(affectedRows, 1 seconds) mustBe 1
      val allItems = Await.result(repo.all(), 1 seconds)
      allItems.length mustBe 1 // because earlier test
    }
  }

  "SlickItemRepository#edit(uuid, contents)" should {
    "properly update contents of item" in new Inject {
      lazy val repo = inject[ItemRepository]

      val affectedRows = for {
        uuid <- repo.add("to be updated")
        affectedRows <- repo.edit(uuid, "updated contents")
      } yield affectedRows
      Await.result(affectedRows, 1 seconds) mustBe 1
    }

    "do not crash when trying to edit non-existing item" in new Inject {
      lazy val repo = inject[ItemRepository]
      Await.result(repo.edit("bogus", "updated value"), 1 seconds) mustBe 0
    }
  }
}
