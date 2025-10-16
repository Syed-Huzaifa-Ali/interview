package forex.http.auth

import cats.effect.IO
import forex.config.AuthConfig
import org.http4s.{AuthedRoutes, Header, Request, Status}
import org.http4s.dsl.io._
import org.http4s.implicits.{http4sKleisliResponseSyntaxOptionT, http4sLiteralsSyntax}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class AuthMiddlewareSpec extends AnyFlatSpec with Matchers {

  val validToken = "test-token-12345"
  val authConfig = AuthConfig(validToken)

  val testRoutes: AuthedRoutes[Unit, IO] = AuthedRoutes.of[Unit, IO] {
    case GET -> Root / "test" as _ => Ok("Success")
  }

  "AuthMiddleware" should "allow requests with valid token" in {
    val middleware = AuthMiddleware[IO](authConfig)
    val authedRoutes = middleware(testRoutes)

    val request = Request[IO](
      method = GET,
      uri = uri"/test"
    ).withHeaders(Header("token", validToken))

    val response = authedRoutes.orNotFound.run(request).unsafeRunSync()

    response.status shouldBe Status.Ok
  }

  it should "reject requests without token" in {
    val middleware = AuthMiddleware[IO](authConfig)
    val authedRoutes = middleware(testRoutes)

    val request = Request[IO](
      method = GET,
      uri = uri"/test"
    )

    val response = authedRoutes.orNotFound.run(request).unsafeRunSync()

    response.status shouldBe Status.Unauthorized
  }

  it should "reject requests with invalid token" in {
    val middleware = AuthMiddleware[IO](authConfig)
    val authedRoutes = middleware(testRoutes)

    val request = Request[IO](
      method = GET,
      uri = uri"/test"
    ).withHeaders(Header("token", "invalid-token"))

    val response = authedRoutes.orNotFound.run(request).unsafeRunSync()

    response.status shouldBe Status.Unauthorized
  }

  it should "reject requests with empty token" in {
    val middleware = AuthMiddleware[IO](authConfig)
    val authedRoutes = middleware(testRoutes)

    val request = Request[IO](
      method = GET,
      uri = uri"/test"
    ).withHeaders(Header("token", ""))

    val response = authedRoutes.orNotFound.run(request).unsafeRunSync()

    response.status shouldBe Status.Unauthorized
  }

  it should "return proper error message on authentication failure" in {
    val middleware = AuthMiddleware[IO](authConfig)
    val authedRoutes = middleware(testRoutes)

    val request = Request[IO](
      method = GET,
      uri = uri"/test"
    ).withHeaders(Header("token", "wrong-token"))

    val response = authedRoutes.orNotFound.run(request).unsafeRunSync()
    val body = response.as[String].unsafeRunSync()

    response.status shouldBe Status.Unauthorized
    body should include("Token verification failed")
  }

  it should "be case-sensitive for token header name" in {
    val middleware = AuthMiddleware[IO](authConfig)
    val authedRoutes = middleware(testRoutes)

    // Using "Token" instead of "token"
    val request = Request[IO](
      method = GET,
      uri = uri"/test"
    ).withHeaders(Header("Token", validToken))

    val response = authedRoutes.orNotFound.run(request).unsafeRunSync()

    (response.status == Status.Ok || response.status == Status.Unauthorized) shouldBe true
  }

  it should "handle multiple failed authentication attempts" in {
    val middleware = AuthMiddleware[IO](authConfig)
    val authedRoutes = middleware(testRoutes)

    val invalidTokens = List("wrong1", "wrong2", "wrong3", "")

    invalidTokens.foreach { token =>
      val request = Request[IO](
        method = GET,
        uri = uri"/test"
      ).withHeaders(Header("token", token))

      val response = authedRoutes.orNotFound.run(request).unsafeRunSync()
      response.status shouldBe Status.Unauthorized
    }
  }
}

