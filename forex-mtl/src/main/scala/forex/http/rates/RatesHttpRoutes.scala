package forex.http
package rates

import cats.effect.Sync
import cats.syntax.applicativeError._
import cats.syntax.flatMap._
import forex.domain.Currency
import forex.programs.RatesProgram
import forex.programs.rates.{ Protocol => RatesProgramProtocol }
import forex.programs.rates.errors._
import org.http4s.{ AuthedRoutes, HttpRoutes, Response, Status }
import org.http4s.dsl.Http4sDsl
import org.http4s.circe.CirceEntityEncoder._

class RatesHttpRoutes[F[_]: Sync](rates: RatesProgram[F]) extends Http4sDsl[F] {

  import Converters._
  import Protocol._
  import QueryParams._

  private[http] val prefixPath = "/rates"

  // Authed routes that require token
  private val authedRoutes: AuthedRoutes[Unit, F] = AuthedRoutes.of[Unit, F] {
    case GET -> Root :? FromQueryParam(from) +& ToQueryParam(to) as _ =>
      handleGetRate(from, to)

    case GET -> Root as _ =>
      BadRequest(
        ErrorResponse("Invalid request. Please provide both currencies in scope.")
      )
  }

  def routes(authMiddleware: org.http4s.server.AuthMiddleware[F, Unit]): HttpRoutes[F] = {
    org.http4s.server.Router(
      prefixPath -> authMiddleware(authedRoutes)
    )
  }

  private def handleGetRate(from: Currency, to: Currency): F[Response[F]] = {
    // Validate currencies are different
    if (from == to) {
      BadRequest(
        ErrorResponse("Invalid request. Please provide both currencies in scope.")
      )
    } else {
      rates
        .get(RatesProgramProtocol.GetRatesRequest(from, to))
        .flatMap {
          case Right(rate) =>
            Ok(rate.asGetApiResponse)

          case Left(error) =>
            handleError(error)
        }
        .handleError { _ =>
          Response[F](Status.InternalServerError)
            .withEntity(ErrorResponse("Internal error. Please contact the developer."))
        }
    }
  }

  private def handleError(error: Error): F[Response[F]] = error match {
    case Error.ServiceUnavailable(msg) =>
      ServiceUnavailable(ErrorResponse(msg))

    case Error.RateLookupFailed(_) =>
      InternalServerError(
        ErrorResponse("Internal error. Please contact the developer.")
      )

    case Error.InvalidCurrency(msg) =>
      BadRequest(ErrorResponse(msg))
  }
}