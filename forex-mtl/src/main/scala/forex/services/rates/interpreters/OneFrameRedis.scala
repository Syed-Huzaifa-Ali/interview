package forex.services.rates.interpreters

import cats.effect.Concurrent
import cats.syntax.applicative._
import cats.syntax.either._
import cats.syntax.flatMap._
import cats.syntax.functor._
import forex.config.RedisConfig
import forex.domain.{ Currency, Rate }
import forex.services.rates.Algebra
import forex.services.rates.cache.RatesCache
import forex.services.rates.client.OneFrameClient
import forex.services.rates.errors.Error

import java.time.OffsetDateTime

trait OneFrameService[F[_]] extends Algebra[F] {
  def refreshAllRates(): F[Unit]
}

class OneFrameRedis[F[_]: Concurrent](
                                       client: OneFrameClient[F],
                                       cache: RatesCache[F],
                                       config: RedisConfig
                                     ) extends OneFrameService[F] {

  private val allCurrencies: List[Currency] = List(
    Currency.AUD,
    Currency.CAD,
    Currency.CHF,
    Currency.EUR,
    Currency.GBP,
    Currency.NZD,
    Currency.JPY,
    Currency.SGD,
    Currency.USD
  )

  private val allPairs: List[Rate.Pair] = for {
    from <- allCurrencies
    to   <- allCurrencies
    if from != to
  } yield Rate.Pair(from, to)

  override def get(pair: Rate.Pair): F[Error Either Rate] = {
    cache.get(pair).flatMap {
      case Some(rate) if isFresh(rate) =>
        rate.asRight[Error].pure[F]

      case Some(rate) =>
        // Rate exists but is stale - try to refresh
        refreshRate(pair).flatMap {
          case Right(freshRate) => freshRate.asRight[Error].pure[F]
          case Left(_)          => rate.asRight[Error].pure[F] // Return stale on failure
        }

      case None =>
        // No cached rate - must fetch
        refreshRate(pair)
    }
  }

  override def refreshAllRates(): F[Unit] = {
    client.getMultiple(allPairs).flatMap {
      case Right(rates) =>
        cache.putMultiple(rates)
      case Left(_) =>
        Concurrent[F].unit // Log error in production
    }
  }

  private def refreshRate(pair: Rate.Pair): F[Error Either Rate] = {
    client.get(pair).flatMap {
      case Right(rate) =>
        cache.put(pair, rate).map(_ => rate.asRight[Error])
      case Left(error) =>
        error.asLeft[Rate].pure[F]
    }
  }

  private def isFresh(rate: Rate): Boolean = {
    val now = OffsetDateTime.now()
    val age = java.time.Duration.between(rate.timestamp.value, now)
    age.toMillis < config.cacheTtl.toMillis
  }
}

object OneFrameRedis {
  def apply[F[_]: Concurrent](
                               client: OneFrameClient[F],
                               cache: RatesCache[F],
                               config: RedisConfig
                             ): OneFrameService[F] = new OneFrameRedis[F](client, cache, config)
}