package services

import pdi.jwt.{JwtJson, JwtAlgorithm, JwtClaim}
import play.api.Logging

import scala.util.{Failure, Success}

object JwtValidator extends Logging {
  private val secret = sys.env("JWT_SECRET") // Shared secret for signing and verifying tokens
  private val expectedAudience = Set("listan") // Define your audience

  def validateToken(token: String): Either[String, JwtClaim] = {
    JwtJson.decode(token, secret, Seq(JwtAlgorithm.HS256)) match {
      case Success(claim: JwtClaim) =>
        if (!claim.expiration.exists(_ > System.currentTimeMillis() / 1000))
          Left("Token expired")
        else if (!claim.audience.contains(expectedAudience))
          Left("Invalid audience")
        else
          Right(claim)
      case Failure(exception) =>
        logger.error(s"Token decoding failed: ${exception.getMessage}")
        Left(s"Token validation failed: ${exception.getMessage}")
    }
  }
}
