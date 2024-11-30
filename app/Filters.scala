import javax.inject.Inject

import play.api.http.HttpFilters
import play.filters.cors.CORSFilter
import play.api.Mode 
import play.api.Environment
import play.api.Logging

class Filters @Inject() (env: Environment, corsFilter: CORSFilter) extends HttpFilters with Logging {
  def filters = Seq(corsFilter)
}
