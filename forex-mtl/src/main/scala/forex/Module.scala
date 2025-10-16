package forex

import cats.effect.{ Concurrent, Timer }
import dev.profunktor.redis4cats.RedisCommands
import forex.config.ApplicationConfig
import forex.http.auth.AuthMiddleware
import forex.http.rates.RatesHttpRoutes
import forex.programs._
import forex.services.rates.cache.RatesCache
import forex.services.rates.client.OneFrameClient
import forex.services.rates.interpreters.{ OneFrameRedis, OneFrameService }
import org.http4s._
import org.http4s.client.Client
import org.http4s.implicits._
import org.http4s.server.middleware.{ AutoSlash, Timeout }

class Module[F[_]: Concurrent: Timer](
                                       config: ApplicationConfig,
                                       httpClient: Client[F],
                                       redis: RedisCommands[F, String, String]
                                     ) {

  // Create One-Frame client
  private val oneFrameClient: OneFrameClient[F] =
    OneFrameClient[F](config.oneFrame, httpClient)

  // Create Redis cache
  private val cache: RatesCache[F] =
    RatesCache.redis[F](redis, config.redis.cacheTtl)

  // Create rates service
  val ratesService: OneFrameService[F] =
    OneFrameRedis[F](oneFrameClient, cache, config.redis)

  // Create rates program
  private val ratesProgram: RatesProgram[F] =
    RatesProgram[F](ratesService)

  // Create authentication middleware
  private val authMiddleware: org.http4s.server.AuthMiddleware[F, Unit] =
    AuthMiddleware[F](config.auth)

  // Create HTTP routes
  private val ratesHttpRoutes: HttpRoutes[F] =
    new RatesHttpRoutes[F](ratesProgram).routes(authMiddleware)

  // Middleware types
  type PartialMiddleware = HttpRoutes[F] => HttpRoutes[F]
  type TotalMiddleware   = HttpApp[F] => HttpApp[F]

  // Apply route middleware
  private val routesMiddleware: PartialMiddleware = {
    { http: HttpRoutes[F] =>
      AutoSlash(http)
    }
  }

  // Apply app middleware
  private val appMiddleware: TotalMiddleware = { http: HttpApp[F] =>
    Timeout(config.http.timeout)(http)
  }

  // Combine routes
  private val http: HttpRoutes[F] = ratesHttpRoutes

  // Final HTTP application
  val httpApp: HttpApp[F] = appMiddleware(routesMiddleware(http).orNotFound)
}