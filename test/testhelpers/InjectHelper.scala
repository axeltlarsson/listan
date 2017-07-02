package testhelpers

import play.api.inject.Injector
import play.api.inject.guice.GuiceApplicationBuilder

import scala.concurrent.ExecutionContext

/* Provides injector and ec */
trait InjectHelper {
  lazy val injector = (new GuiceApplicationBuilder).injector()
}

