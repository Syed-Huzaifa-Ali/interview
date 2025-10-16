package forex.config

import cats.effect.IO
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.concurrent.duration._

class ConfigSpec extends AnyFlatSpec with Matchers {

  "Config.stream" should "load configuration from application.conf" in {
    val configStream = Config.stream[IO]("app")
    val config = configStream.compile.lastOrError.unsafeRunSync()

    config shouldBe a[ApplicationConfig]
    config.http shouldBe a[HttpConfig]
    config.auth shouldBe a[AuthConfig]
    config.oneFrame shouldBe a[OneFrameConfig]
    config.redis shouldBe a[RedisConfig]
  }

  it should "load HTTP configuration correctly" in {
    val configStream = Config.stream[IO]("app")
    val config = configStream.compile.lastOrError.unsafeRunSync()

    config.http.host should not be empty
    config.http.port should be > 0
    config.http.timeout should be > 0.seconds
  }

  it should "load auth configuration" in {
    val configStream = Config.stream[IO]("app")
    val config = configStream.compile.lastOrError.unsafeRunSync()

    config.auth.token should not be empty
  }

  it should "load One-Frame configuration" in {
    val configStream = Config.stream[IO]("app")
    val config = configStream.compile.lastOrError.unsafeRunSync()

    config.oneFrame.baseUrl should not be empty
    config.oneFrame.token should not be empty
    config.oneFrame.requestTimeout should be > 0.milliseconds
    config.oneFrame.connectTimeout should be > 0.milliseconds
  }

  it should "load Redis configuration" in {
    val configStream = Config.stream[IO]("app")
    val config = configStream.compile.lastOrError.unsafeRunSync()

    config.redis.uri should not be empty
    config.redis.cacheTtl should be > 0.minutes
    config.redis.refreshInterval should be > 0.minutes
  }

  "HttpConfig" should "have reasonable defaults" in {
    val configStream = Config.stream[IO]("app")
    val config = configStream.compile.lastOrError.unsafeRunSync()

    config.http.port should (be >= 1024 and be <= 65535)
    config.http.timeout should be >= 1.second
  }

  "RedisConfig" should "have cache TTL of 5 minutes" in {
    val configStream = Config.stream[IO]("app")
    val config = configStream.compile.lastOrError.unsafeRunSync()

    config.redis.cacheTtl shouldBe 5.minutes
    config.redis.refreshInterval shouldBe 5.minutes
  }
}