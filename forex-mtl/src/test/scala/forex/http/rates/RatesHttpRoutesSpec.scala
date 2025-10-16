package forex.http.rates

import cats.effect.IO
import forex.config.AuthConfig
import forex.domain.{Currency, Price, Rate, Timestamp}
import forex.http.auth.AuthMiddleware
import forex.programs.rates.{Algebra, Protocol => RatesProgramProtocol}
import forex.programs.rates.errors._
import io.circe.parser._
import org.http4s.{Header, Request, Status}
import org.http4s.dsl.io._
import org.http4s.implicits.{http4sKleisliResponseSyntaxOptionT, http4sLiteralsSyntax}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import java.time.OffsetDateTime

class RatesHttpRoutesSpec extends AnyFlatSpec with Matchers {

  val validToken = "test-token"
  val authConfig = AuthConfig(validToken)
  val authMiddleware = AuthMiddleware[IO](authConfig)

  class MockRatesProgram(response: Error Either Rate) extends Algebra[IO] {
    override def get(request: RatesProgramProtocol.GetRatesRequest): IO[Error Either Rate] = {
      IO.pure(response)
    }
  }

  "GET /rates" should "return 200 with valid token and parameters" in {
    val rate = Rate(
      Rate.Pair(Currency.USD, Currency.EUR),
      Price(BigDecimal("1.2345")),
      Timestamp(OffsetDateTime.now())
    )
    val program = new MockRatesProgram(Right(rate))
    val routes = new RatesHttpRoutes[IO](program).routes(authMiddleware)

    val request = Request[IO](
      method = GET,
      uri = uri"/rates?from=USD&to=EUR"
    ).withHeaders(Header("token", validToken))

    val response = routes.orNotFound.run(request).unsafeRunSync()

    assert(response.status == Status.Ok)

    val body = response.as[String].unsafeRunSync()
    assert(body.contains("USD"))
    assert(body.contains("EUR"))
    assert(body.contains("price"))
  }

  it should "return 401 without token" in {
    val rate = Rate(
      Rate.Pair(Currency.USD, Currency.EUR),
      Price(BigDecimal("1.2345")),
      Timestamp(OffsetDateTime.now())
    )
    val program = new MockRatesProgram(Right(rate))
    val routes = new RatesHttpRoutes[IO](program).routes(authMiddleware)

    val request = Request[IO](
      method = GET,
      uri = uri"/rates?from=USD&to=EUR"
    )

    val response = routes.orNotFound.run(request).unsafeRunSync()

    assert(response.status == Status.Unauthorized)
    val body = response.as[String].unsafeRunSync()
    assert(body.contains("Token verification failed"))
  }

  it should "return 401 with invalid token" in {
    val rate = Rate(
      Rate.Pair(Currency.USD, Currency.EUR),
      Price(BigDecimal("1.2345")),
      Timestamp(OffsetDateTime.now())
    )
    val program = new MockRatesProgram(Right(rate))
    val routes = new RatesHttpRoutes[IO](program).routes(authMiddleware)

    val request = Request[IO](
      method = GET,
      uri = uri"/rates?from=USD&to=EUR"
    ).withHeaders(Header("token", "wrong-token"))

    val response = routes.orNotFound.run(request).unsafeRunSync()

    assert(response.status == Status.Unauthorized)
  }

  it should "return 400 when from parameter is missing" in {
    val rate = Rate(
      Rate.Pair(Currency.USD, Currency.EUR),
      Price(BigDecimal("1.2345")),
      Timestamp(OffsetDateTime.now())
    )
    val program = new MockRatesProgram(Right(rate))
    val routes = new RatesHttpRoutes[IO](program).routes(authMiddleware)

    val request = Request[IO](
      method = GET,
      uri = uri"/rates?to=EUR"
    ).withHeaders(Header("token", validToken))

    val response = routes.orNotFound.run(request).unsafeRunSync()

    assert(response.status == Status.BadRequest)
  }

  it should "return 400 when to parameter is missing" in {
    val rate = Rate(
      Rate.Pair(Currency.USD, Currency.EUR),
      Price(BigDecimal("1.2345")),
      Timestamp(OffsetDateTime.now())
    )
    val program = new MockRatesProgram(Right(rate))
    val routes = new RatesHttpRoutes[IO](program).routes(authMiddleware)

    val request = Request[IO](
      method = GET,
      uri = uri"/rates?from=USD"
    ).withHeaders(Header("token", validToken))

    val response = routes.orNotFound.run(request).unsafeRunSync()

    assert(response.status == Status.BadRequest)
  }

  it should "return 400 when both parameters are missing" in {
    val rate = Rate(
      Rate.Pair(Currency.USD, Currency.EUR),
      Price(BigDecimal("1.2345")),
      Timestamp(OffsetDateTime.now())
    )
    val program = new MockRatesProgram(Right(rate))
    val routes = new RatesHttpRoutes[IO](program).routes(authMiddleware)

    val request = Request[IO](
      method = GET,
      uri = uri"/rates"
    ).withHeaders(Header("token", validToken))

    val response = routes.orNotFound.run(request).unsafeRunSync()

    assert(response.status == Status.BadRequest)
    val body = response.as[String].unsafeRunSync()
    assert(body.contains("Invalid request"))
  }

  it should "return 503 when service is unavailable" in {
    val program = new MockRatesProgram(Left(
      Error.ServiceUnavailable("Unable to reach external rate service. Please try again later.")
    ))
    val routes = new RatesHttpRoutes[IO](program).routes(authMiddleware)

    val request = Request[IO](
      method = GET,
      uri = uri"/rates?from=USD&to=EUR"
    ).withHeaders(Header("token", validToken))

    val response = routes.orNotFound.run(request).unsafeRunSync()

    assert(response.status == Status.ServiceUnavailable)
    val body = response.as[String].unsafeRunSync()
    assert(body.contains("Unable to reach external rate service"))
  }

  it should "return 500 for rate lookup failure" in {
    val program = new MockRatesProgram(Left(
      Error.RateLookupFailed("Internal error")
    ))
    val routes = new RatesHttpRoutes[IO](program).routes(authMiddleware)

    val request = Request[IO](
      method = GET,
      uri = uri"/rates?from=USD&to=EUR"
    ).withHeaders(Header("token", validToken))

    val response = routes.orNotFound.run(request).unsafeRunSync()

    assert(response.status == Status.InternalServerError)
    val body = response.as[String].unsafeRunSync()
    assert(body.contains("Internal error"))
  }

  it should "handle case-insensitive currency codes" in {
    val rate = Rate(
      Rate.Pair(Currency.USD, Currency.EUR),
      Price(BigDecimal("1.2345")),
      Timestamp(OffsetDateTime.now())
    )
    val program = new MockRatesProgram(Right(rate))
    val routes = new RatesHttpRoutes[IO](program).routes(authMiddleware)

    val request = Request[IO](
      method = GET,
      uri = uri"/rates?from=usd&to=eur"
    ).withHeaders(Header("token", validToken))

    val response = routes.orNotFound.run(request).unsafeRunSync()

    assert(response.status == Status.Ok)
  }

  it should "return proper JSON structure" in {
    val rate = Rate(
      Rate.Pair(Currency.USD, Currency.EUR),
      Price(BigDecimal("1.2345")),
      Timestamp(OffsetDateTime.parse("2024-01-01T12:00:00Z"))
    )
    val program = new MockRatesProgram(Right(rate))
    val routes = new RatesHttpRoutes[IO](program).routes(authMiddleware)

    val request = Request[IO](
      method = GET,
      uri = uri"/rates?from=USD&to=EUR"
    ).withHeaders(Header("token", validToken))

    val response = routes.orNotFound.run(request).unsafeRunSync()
    val body = response.as[String].unsafeRunSync()
    val json = parse(body).getOrElse(fail("Invalid JSON"))

    val cursor = json.hcursor
    assert(cursor.downField("from").as[String] == Right("USD"))
    assert(cursor.downField("to").as[String] == Right("EUR"))
    assert(cursor.downField("price").as[BigDecimal] == Right(BigDecimal("1.2345")))
    assert(cursor.downField("timestamp").as[String].isRight)
  }

  it should "handle all valid currency pairs" in {
    val currencies = List(Currency.USD, Currency.EUR, Currency.GBP, Currency.JPY)

    currencies.foreach { from =>
      currencies.foreach { to =>
        if (from != to) {
          val rate = Rate(Rate.Pair(from, to), Price(BigDecimal("1.5")), Timestamp.now)
          val program = new MockRatesProgram(Right(rate))
          val routes = new RatesHttpRoutes[IO](program).routes(authMiddleware)

          val request = Request[IO](
            method = GET,
            uri = uri"/rates".withQueryParam("from", Currency.show.show(from))
              .withQueryParam("to", Currency.show.show(to))
          ).withHeaders(Header("token", validToken))

          val response = routes.orNotFound.run(request).unsafeRunSync()
          assert(response.status == Status.Ok)
        }
      }
    }
  }
}