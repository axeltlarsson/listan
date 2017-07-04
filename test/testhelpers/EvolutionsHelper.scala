package testhelpers

import play.api.db.DBApi
import play.api.db.evolutions.Evolutions
import play.api.inject.guice.GuiceApplicationBuilder

trait EvolutionsHelper {
  // Require an Injector to use this

  def clean() = {
    val injector = new GuiceApplicationBuilder().injector
    val dbApi = injector.instanceOf[DBApi]
    Evolutions.cleanupEvolutions(dbApi.database("test"))
  }

  def evolve() = {
    val injector = new GuiceApplicationBuilder().injector
    val dbApi = injector.instanceOf[DBApi]
    Evolutions.applyEvolutions(dbApi.database("test"))
  }
}
