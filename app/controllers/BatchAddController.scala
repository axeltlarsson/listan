package controllers

import javax.inject._
import play.api.mvc._
import play.api.libs.json.Json
import services.JwtValidator

@Singleton
class BatchAddController @Inject()(cc: ControllerComponents) extends AbstractController(cc) {

  def batchAdd: Action[AnyContent] = Action { request =>
    // Extract the Authorization header
    request.headers.get("Authorization") match {
      case Some(authHeader) =>
        val token = authHeader.replace("Bearer ", "")
        JwtValidator.validateToken(token) match {
          case Right(claim) =>
            // Parse the claim content as JSON
            val userId = (Json.parse(claim.content) \ "sub").asOpt[String].getOrElse("unknown_user")
            Ok(Json.obj("message" -> s"Exporting ingredients for user $userId"))
          case Left(error) =>
            Unauthorized(Json.obj("error" -> error.toString)) // Ensure error is a String
        }

      case None =>
        Unauthorized(Json.obj("error" -> "Missing Authorization header"))
    }
  }
}
