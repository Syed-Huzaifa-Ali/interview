package forex.services.rates.client

import cats.effect.{ Concurrent, Timer }
import cats.syntax.either._
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.syntax.applicativeError._
import forex.config.OneFrameConfig
import forex.domain.{ Currency, Price, Rate, Timestamp }
import forex.services.rates.errors.Error
import io.circe.generic.semiauto.deriveDecoder
import io.circe.Decoder
import org.http4s.circe.CirceEntityDecoder._
import org.http4s.client.Client
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.{ Header, Method, Request, Uri }

import java.time.OffsetDateTime

trait OneFrameClient[F[_]] {
  def get(pair: Rate.Pair): F[Error Either Rate]
  def getMultiple(pairs: List[Rate.Pair]): F[Error Either List[Rate]]
}

object OneFrameClient {

  def apply[F[_]: Concurrent: Timer](
                                      config: OneFrameConfig,
                                      client: Client[F]
                                    ): OneFrameClient[F] = new OneFrameClientImpl[F](config, client)

  private class OneFrameClientImpl[F[_]: Concurrent: Timer](
                                                             config: OneFrameConfig,
                                                             client: Client[F]
                                                           ) extends OneFrameClient[F]
    with Http4sClientDsl[F] {

    private case class OneFrameRate(
                                     from: String,
                                     to: String,
                                     bid: BigDecimal,
                                     ask: BigDecimal,
                                     price: BigDecimal,
                                     time_stamp: OffsetDateTime
                                   )

    private implicit val oneFrameRateDecoder: Decoder[OneFrameRate] = deriveDecoder[OneFrameRate]

    override def get(pair: Rate.Pair): F[Error Either Rate] =
      getMultiple(List(pair)).map(_.map(_.head))

    override def getMultiple(pairs: List[Rate.Pair]): F[Error Either List[Rate]] = {
      if (pairs.isEmpty) {
        Concurrent[F].pure(List.empty[Rate].asRight[Error])
      } else {
        val pairStrings = pairs.map(p => s"${Currency.show.show(p.from)}${Currency.show.show(p.to)}")
        fetchRates(pairStrings).map { result =>
          result.map { oneFrameRates =>
            oneFrameRates.map(toRate)
          }
        }
      }
    }

    private def fetchRates(pairs: List[String]): F[Error Either List[OneFrameRate]] = {
      val baseUri = Uri.unsafeFromString(config.baseUrl)

      val queryParams = pairs.map(p => "pair" -> p)
      val uriWithParams = queryParams.foldLeft(baseUri.withPath("/rates")) { case (uri, (key, value)) =>
        uri.withQueryParam(key, value)
      }

      val request = Request[F](
        method = Method.GET,
        uri = uriWithParams,
        headers = org.http4s.Headers.of(Header("token", config.token))
      )

      val timeoutRequest = Timer[F].sleep(config.requestTimeout) >>
        Concurrent[F].raiseError[List[OneFrameRate]](
          new java.util.concurrent.TimeoutException(s"Request timeout after ${config.requestTimeout}")
        )

      Concurrent[F].race(
        client.expectOr[List[OneFrameRate]](request) { response =>
          response.as[String].map { body =>
            new RuntimeException(s"One-Frame API error: ${response.status}, body: $body")
          }
        },
        timeoutRequest
      ).map {
        case Left(rates) => rates.asRight[Error]
        case Right(_)    => Error.OneFrameLookupFailed("Request timeout").asLeft[List[OneFrameRate]]
      }.handleError { error =>
        Error.OneFrameLookupFailed(error.getMessage).asLeft[List[OneFrameRate]]
      }
    }

    private def toRate(ofRate: OneFrameRate): Rate = {
      Rate(
        pair = Rate.Pair(
          from = Currency.fromString(ofRate.from),
          to = Currency.fromString(ofRate.to)
        ),
        price = Price(ofRate.price),
        timestamp = Timestamp(ofRate.time_stamp)
      )
    }
  }
}