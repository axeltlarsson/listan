package testhelpers

import play.api.db.DBApi
import play.api.db.evolutions.Evolutions

trait EvolutionsHelper {
  // Require an Injector to use this
  this: InjectHelper =>

  def clean() = {
    val dbApi = injector.instanceOf[DBApi]
    Evolutions.cleanupEvolutions(dbApi.database("test"))
  }

  def evolve() = {
    val dbApi = injector.instanceOf[DBApi]
    Evolutions.applyEvolutions(dbApi.database("test"))
  }
}
