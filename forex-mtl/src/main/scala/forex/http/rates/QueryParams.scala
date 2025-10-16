package forex.http.rates

import cats.syntax.either._
import forex.domain.Currency
import org.http4s.{ ParseFailure, QueryParamDecoder }
import org.http4s.dsl.impl.QueryParamDecoderMatcher

object QueryParams {

  private val validCurrencies = Set("AUD", "CAD", "CHF", "EUR", "GBP", "NZD", "JPY", "SGD", "USD")

  private[http] implicit val currencyQueryParam: QueryParamDecoder[Currency] =
    QueryParamDecoder[String].emap { str =>
      val upperStr = str.toUpperCase
      if (validCurrencies.contains(upperStr)) {
        Currency.fromString(upperStr).asRight[ParseFailure]
      } else {
        ParseFailure(
          s"Invalid currency: $str. Valid currencies are: ${validCurrencies.mkString(", ")}",
          s"Currency $str not in scope"
        ).asLeft[Currency]
      }
    }

  object FromQueryParam extends QueryParamDecoderMatcher[Currency]("from")
  object ToQueryParam extends QueryParamDecoderMatcher[Currency]("to")
}