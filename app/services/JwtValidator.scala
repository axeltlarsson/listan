import pdi.jwt._
import scala.util.{Success, Failure}

object JwtValidator {
  private val secret = sys.env("JWT_SECRET") // Shared secret

  def validateToken(token: String): Either[String, JwtClaim] = {
    Jwt.decode(token, secret, Seq(JwtAlgorithm.HS256)) match {
      case Success(claims) =>
        // Validate claims
        val isValid = claims.audience.contains(Set("listan")) &&
                      claims.expiration.forall(_.isAfterNow)
        if (isValid) Right(claims)
        else Left("Invalid claims")

      case Failure(e) =>
        Left(s"Token validation failed: ${e.getMessage}")
    }
  }
}
