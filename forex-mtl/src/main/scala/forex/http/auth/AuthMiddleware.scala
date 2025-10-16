package forex.http.auth

import cats.data.{ Kleisli, OptionT }
import cats.effect.Sync
import cats.syntax.applicative._
import forex.config.AuthConfig
import io.circe.generic.semiauto.deriveEncoder
import io.circe.Encoder
import io.circe.syntax._
import org.http4s.{ AuthedRoutes, Request, Response, Status }
import org.http4s.circe._
import org.http4s.util.CaseInsensitiveString

object AuthMiddleware {

  def apply[F[_]: Sync](config: AuthConfig): org.http4s.server.AuthMiddleware[F, Unit] = {

    val authUser: Kleisli[F, Request[F], Either[String, Unit]] =
      Kleisli { request =>
        val tokenHeader = request.headers.get(CaseInsensitiveString("token"))

        val result = tokenHeader match {
          case Some(token) if token.value == config.token =>
            Right(())
          case _ =>
            Left("Token verification failed")
        }

        Sync[F].pure(result)
      }

    val onFailure: AuthedRoutes[String, F] =
      Kleisli { _ =>
        OptionT.liftF(
          Response[F](Status.Unauthorized)
            .withEntity(
              ErrorResponse(
                "Token verification failed. Please provide the correct token."
              ).asJson
            )
            .pure[F]
        )
      }

    org.http4s.server.AuthMiddleware(authUser, onFailure)
  }

  case class ErrorResponse(error: String)

  object ErrorResponse {
    implicit val errorResponseEncoder: Encoder[ErrorResponse] = deriveEncoder[ErrorResponse]
  }
}