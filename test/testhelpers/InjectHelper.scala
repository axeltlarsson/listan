package testhelpers

import play.api.inject.guice.GuiceApplicationBuilder

trait InjectHelper {
  lazy val injector = (new GuiceApplicationBuilder).injector
}

