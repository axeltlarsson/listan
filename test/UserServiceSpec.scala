import org.scalatestplus.play._
import org.scalatest.mockito.MockitoSugar
import org.mockito.Mockito._

import play.api.inject.guice.GuiceApplicationBuilder
import play.api.inject.bind
import scala.concurrent.{Future, Await}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps


import models.{User, UserRepository}
import services.{UserService}

class UserServiceSpec extends PlaySpec with MockitoSugar  {

  trait MockUserRepo {
    val mockUserRepo = mock[UserRepository]
    when(mockUserRepo.authenticate("axel", "password")) thenReturn Future{Some(User("axel"))}
    when(mockUserRepo.authenticate("axel", "wrong")) thenReturn Future{None}
    val app = new GuiceApplicationBuilder()
      .overrides(bind[UserRepository].toInstance(mockUserRepo))
      .build
    val userService = app.injector.instanceOf[UserService]
  }

  "UserService#authenticate" should {
    "return correct user for valid password" in new MockUserRepo {
      val maybeUser = Await.result(userService.authenticate("axel", "password"), 1 seconds)
      maybeUser mustBe Some(User("axel"))
    }

    "return None for user with invalid password" in new MockUserRepo {
      val maybeUser = Await.result(userService.authenticate("axel", "wrong"), 1 seconds)
      maybeUser mustBe None
    }
  }

  "SlickUserRepository" should {
    "work with a test db" in new Inject {
      val repo = inject[UserRepository]
      val user = User.create("axel", "whatever")
      repo.insert(user)
      val usersInDb = Await.result(repo.all(), 1 seconds)
      usersInDb(0).name mustBe "axel"
      usersInDb(0).passwordHash must not be Some("whatever") // should be hashed duh
    }
  }
}
