import org.scalatestplus.play._
import org.scalatest.mockito.MockitoSugar
import org.mockito.Mockito._
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.inject.{Injector, bind}

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps
import models.{User, UserRepository}
import org.scalatest.BeforeAndAfterAll
import org.scalatestplus.play.guice.{GuiceOneAppPerSuite, GuiceOneServerPerSuite}
import services.UserService
import testhelpers.{EvolutionsHelper, ListHelper}

class UserServiceSpec extends PlaySpec with MockitoSugar with GuiceOneAppPerSuite with BeforeAndAfterAll with EvolutionsHelper {

  // Clean up db after ourselves
  override def afterAll() = {
    clean()
    evolve()
  }

  trait MockUserRepo {
    val mockUserRepo = mock[UserRepository]
    val app = new GuiceApplicationBuilder()
      .overrides(bind[UserRepository].toInstance(mockUserRepo))
      .build
    val userService = app.injector.instanceOf[UserService]
    implicit val ec: ExecutionContext = app.injector.instanceOf[ExecutionContext]
    when(mockUserRepo.authenticate("axel", "password")) thenReturn Future{Some(User(name = "axel"))}
    when(mockUserRepo.authenticate("axel", "wrong")) thenReturn Future{None}
  }

  "UserService#authenticate" should {
    "return correct user for valid password" in new MockUserRepo {
      val maybeUser = Await.result(userService.authenticate("axel", "password"), 1 seconds)
      maybeUser mustBe Some(User(name = "axel"))
    }

    "return None for user with invalid password" in new MockUserRepo {
      val maybeUser = Await.result(userService.authenticate("axel", "wrong"), 1 seconds)
      maybeUser mustBe None
    }
  }

  "SlickUserRepository" should {
    "work with a test db" in {
      val repo = app.injector.instanceOf[UserRepository]
      val user = User.create("axel", "whatever")
      val uuid = Await.result(repo.insert(user), 100 millis)
      val usersInDb = Await.result(repo.all(), 100 millis)
      usersInDb(0).name mustBe "axel"
      usersInDb(0).passwordHash must not be Some("whatever") // should be hashed duh
    }
  }
}
