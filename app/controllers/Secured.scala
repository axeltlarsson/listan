package controllers

import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits._
import play.api.mvc._
import play.api.mvc.Results._
import play.api.libs.json._
import pdi.jwt._
import models.User

class AuthenticatedRequest[A](val user: User, request: Request[A])
  extends WrappedRequest[A](request)

trait Secured {
  def Authenticated = AuthenticatedAction
}

object AuthenticatedAction extends ActionBuilder[AuthenticatedRequest] {
  def invokeBlock[A](
      request: Request[A],
      block: AuthenticatedRequest[A] => Future[Result]) =
    request.jwtSession.getAs[User]("user") match {
      case Some(user) => block(new AuthenticatedRequest(user, request)).map(
        _.refreshJwtSession(request))
      case _ => Future.successful(Unauthorized)
    }
}
