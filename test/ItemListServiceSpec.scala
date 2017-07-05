import org.scalatest.Matchers._
import java.sql.Timestamp
import models.{ItemList, User, UserRepository}
import org.scalatest.BeforeAndAfter
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.inject.guice.GuiceApplicationBuilder
import services.ItemListService
import testhelpers.EvolutionsHelper

import scala.language.postfixOps
import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration._

class ItemListServiceSpec extends PlaySpec with GuiceOneAppPerSuite with BeforeAndAfter with EvolutionsHelper {
  val injector = (new GuiceApplicationBuilder()).injector
  implicit val ec: ExecutionContext = injector.instanceOf[ExecutionContext]
  val service = injector.instanceOf[ItemListService]

  before {
    evolve()
    val userRepo = injector.instanceOf[UserRepository]
    // Insert users and await completion
    val uuidFutures = for {
      uuid1 <- userRepo.insert(User.create("user1", "password1"))
      uuid2 <- userRepo.insert(User.create("user2", "password2"))
    } yield (uuid1, uuid2)
    Await.result(uuidFutures, 300 millis)
 }

  after {
    clean()
  }

  trait Users {
    // Get the users
    private val userRepo = injector.instanceOf[UserRepository]
    private val usersFuture = for {
      user1 <- userRepo.authenticate("user1", "password1")
      user2 <- userRepo.authenticate("user2", "password2")
    } yield (user1, user2)
    val users = Await.result(usersFuture, 300 millis)
  }

  "SlickItemListRepository" should {
    "handle creation, updates and deletion of a list" in new Users {
      val creationTime = new Timestamp(System.currentTimeMillis())
      val uuid1 = Await.result(service.add(name = "list1", user = users._1.get), 10 millis)
      uuid1.length must be > 20
      val all = Await.result(service.all(), 10 millis)
      all must have length(1)
      all(0).created mustBe defined
      all(0).created.foreach(_.getTime() shouldEqual creationTime.getTime() +- 20)
      all(0).updated mustBe defined
      all(0).updated.foreach(_.getTime() shouldEqual creationTime.getTime() +- 20)
      all(0).description mustBe None
      all(0).name mustBe "list1"
      all(0).userUuid mustBe users._1.get.uuid.get


      // Update description
      val updateTime = new Timestamp(System.currentTimeMillis())
      val isSuccessful = Await.result(service.updateDescription(description = "new descr", uuid = uuid1), 10 millis)
      isSuccessful mustBe true
      val list1 = Await.result(service.all(), 10 millis).find(_.uuid == Some(uuid1))
      list1 mustBe defined
      list1.foreach(l => {
        l.description mustBe Some("new descr")
        l.name mustBe "list1"
        l.created mustBe defined
        l.created.foreach(_.getTime() shouldEqual creationTime.getTime() +- 20)
        l.updated mustBe defined
        l.updated.foreach(_.getTime() shouldEqual updateTime.getTime() +- 20)
        l.userUuid mustBe users._1.get.uuid.get
      })

      // Update name
      val successfulNameUpdate = Await.result(service.updateName("new name", uuid1), 10 millis)
      successfulNameUpdate mustBe true
      val list1Again = Await.result(service.all(), 10 millis).find(_.uuid == Some(uuid1))
      list1Again mustBe defined
      list1Again.foreach(_.name mustBe "new name")

      // Delete
      val successfulDeletion = Await.result(service.delete(uuid1), 10 millis)
      successfulDeletion mustBe true
      val allAgainAgain = Await.result(service.all(), 10 millis)
      allAgainAgain must have length(0)
    }

    "not crash when trying to delete non-existent list" in {
      val successfulDeletion = Await.result(service.delete("i-do-not-exist"), 10 millis)
      successfulDeletion mustBe false
    }

  }
}

