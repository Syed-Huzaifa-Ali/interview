package forex

import cats.effect._
import dev.profunktor.redis4cats.Redis
import dev.profunktor.redis4cats.effect.Log
import forex.config.Config
import fs2.Stream
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.server.blaze.BlazeServerBuilder

import scala.concurrent.ExecutionContext

object Main extends IOApp {

  override def run(args: List[String]): IO[ExitCode] =
    new Application[IO].stream(executionContext).compile.drain.as(ExitCode.Success)
}

class Application[F[_]: ConcurrentEffect: Timer: ContextShift] {

  // Implicit Log instance for Redis (no-op logger)
  implicit val log: Log[F] = new Log[F] {
    override def info(msg: => String): F[Unit] = ConcurrentEffect[F].unit
    override def error(msg: => String): F[Unit] = ConcurrentEffect[F].unit
    override def debug(msg: => String): F[Unit] = ConcurrentEffect[F].unit
  }

  def stream(ec: ExecutionContext): Stream[F, Unit] =
    for {
      config <- Config.stream("app")

      // Create HTTP client for One-Frame API
      httpClient <- Stream.resource(BlazeClientBuilder[F](ec).resource)

      // Create Redis client
      redis <- Stream.resource(Redis[F].utf8(config.redis.uri))

      // Create module with all dependencies
      module = new Module[F](config, httpClient, redis)

      // Background refresh job - sleep then refresh repeatedly
      refreshJob = Stream.fixedDelay[F](config.redis.refreshInterval)
        .evalMap(_ => module.ratesService.refreshAllRates())

      // Start server and refresh job concurrently
      _ <- Stream(
        BlazeServerBuilder[F](ec)
          .bindHttp(config.http.port, config.http.host)
          .withHttpApp(module.httpApp)
          .serve
          .drain,
        refreshJob
      ).parJoinUnbounded

    } yield ()
}