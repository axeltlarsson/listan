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
import scala.concurrent.{Future, Await}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps
import play.api.db.slick.DatabaseConfigProvider

import play.api.inject.BindingKey
import play.api.inject.QualifierInstance
import play.db.NamedDatabaseImpl


import models.{User, UserRepository, SlickUserRepository}
import services.{UserService}

class UserServiceSpec extends PlaySpec with MockitoSugar {

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
    "work with a test db" in {
      val app = new GuiceApplicationBuilder().build 
      val repo = app.injector.instanceOf[UserRepository]
      val user = User.create("axel", "password")
      repo.insert(user)
      val usersInDb = Await.result(repo.all(), 1 seconds)
      usersInDb(0).name mustBe "axel"
      usersInDb(0).passwordHash must not be Some("password") // should be hashed duh
    }
  }
}
