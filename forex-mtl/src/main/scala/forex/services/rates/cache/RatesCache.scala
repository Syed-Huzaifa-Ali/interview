package forex.services.rates.cache

import cats.effect.Concurrent
import cats.syntax.functor._
import dev.profunktor.redis4cats.RedisCommands
import forex.domain.{ Currency, Rate, Timestamp }
import io.circe.parser._
import io.circe.syntax._
import io.circe.{ Decoder, Encoder }

import java.time.OffsetDateTime
import scala.concurrent.duration.FiniteDuration

trait RatesCache[F[_]] {
  def get(pair: Rate.Pair): F[Option[Rate]]
  def put(pair: Rate.Pair, rate: Rate): F[Unit]
  def putMultiple(rates: List[Rate]): F[Unit]
  def getAll(pairs: List[Rate.Pair]): F[Map[Rate.Pair, Rate]]
}

object RatesCache {

  def redis[F[_]: Concurrent](
                               redis: RedisCommands[F, String, String],
                               cacheTtl: FiniteDuration
                             ): RatesCache[F] = new RedisRatesCache[F](redis, cacheTtl)

  private class RedisRatesCache[F[_]: Concurrent](
                                                   redis: RedisCommands[F, String, String],
                                                   cacheTtl: FiniteDuration
                                                 ) extends RatesCache[F] {

    private case class CachedRate(
                                   fromCurrency: String,
                                   toCurrency: String,
                                   price: BigDecimal,
                                   timestamp: OffsetDateTime
                                 )

    private implicit val cachedRateEncoder: Encoder[CachedRate] =
      io.circe.generic.semiauto.deriveEncoder[CachedRate]
    private implicit val cachedRateDecoder: Decoder[CachedRate] =
      io.circe.generic.semiauto.deriveDecoder[CachedRate]

    private def cacheKey(pair: Rate.Pair): String =
      s"rate:${Currency.show.show(pair.from)}:${Currency.show.show(pair.to)}"

    private def toRate(cached: CachedRate): Rate = {
      Rate(
        pair = Rate.Pair(
          from = Currency.fromString(cached.fromCurrency),
          to = Currency.fromString(cached.toCurrency)
        ),
        price = forex.domain.Price(cached.price),
        timestamp = Timestamp(cached.timestamp)
      )
    }

    private def fromRate(rate: Rate): CachedRate = {
      CachedRate(
        fromCurrency = Currency.show.show(rate.pair.from),
        toCurrency = Currency.show.show(rate.pair.to),
        price = rate.price.value,
        timestamp = rate.timestamp.value
      )
    }

    override def get(pair: Rate.Pair): F[Option[Rate]] = {
      val key = cacheKey(pair)
      redis.get(key).map { maybeJson =>
        maybeJson.flatMap { json =>
          decode[CachedRate](json).toOption.map(toRate)
        }
      }
    }

    override def put(pair: Rate.Pair, rate: Rate): F[Unit] = {
      val key = cacheKey(pair)
      val cached = fromRate(rate)
      val json = cached.asJson.noSpaces
      redis.setEx(key, json, cacheTtl).void
    }

    override def putMultiple(rates: List[Rate]): F[Unit] = {
      import cats.implicits._
      rates.traverse_(rate => put(rate.pair, rate))
    }

    override def getAll(pairs: List[Rate.Pair]): F[Map[Rate.Pair, Rate]] = {
      import cats.implicits._
      pairs.traverse { pair =>
        get(pair).map(maybeRate => maybeRate.map(rate => pair -> rate))
      }.map(_.flatten.toMap)
    }
  }
}