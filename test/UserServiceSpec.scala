import org.scalatestplus.play._
import org.scalatest.mock.MockitoSugar
import org.mockito.Mockito._
import org.mockito.Matchers._
import org.scalatest._

import play.api.test._
import play.api.test.Helpers._
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.inject.{bind, Injector}
import play.api.Configuration

import models.{User, UserRepository, SlickUserRepository}
import services.{UserService}

class UserServiceSpec extends PlaySpec with MockitoSugar {

  trait MockUserRepo {
    val mockUserRepo = mock[UserRepository]
    when(mockUserRepo.authenticate("axel", "password")) thenReturn Some(User("axel"))
    when(mockUserRepo.authenticate("axel", "wrong")) thenReturn None
    val app = new GuiceApplicationBuilder()
      .overrides(bind[UserRepository].toInstance(mockUserRepo))
      .build
    val userService = app.injector.instanceOf[UserService]
  }

  "UserService#authenticate" should {
    "return correct user for valid password" in new MockUserRepo {
      val maybeUser = userService.authenticate("axel", "password")
      maybeUser mustBe Some(User("axel"))
    }

    "return None for user with invalid password" in new MockUserRepo {
      val maybeUser = userService.authenticate("axel", "wrong")
      maybeUser mustBe None
    }
  }
}
