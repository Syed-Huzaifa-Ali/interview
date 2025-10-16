package forex.http
package rates

import forex.domain.Currency.show
import forex.domain._
import io.circe._
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.semiauto.deriveConfiguredEncoder

import java.time.format.DateTimeFormatter

object Protocol {

  implicit val configuration: Configuration = Configuration.default.withSnakeCaseMemberNames

  final case class GetApiRequest(
                                  from: Currency,
                                  to: Currency
                                )

  final case class GetApiResponse(
                                   from: Currency,
                                   to: Currency,
                                   price: BigDecimal,
                                   timestamp: String
                                 )

  final case class ErrorResponse(
                                  error: String
                                )

  // Custom encoder for Currency to output as string
  implicit val currencyEncoder: Encoder[Currency] =
    Encoder.instance[Currency] { currency => Json.fromString(show.show(currency)) }

  // Custom decoder for Currency
  implicit val currencyDecoder: Decoder[Currency] =
    Decoder.instance[Currency] { cursor =>
      cursor.as[String].map(Currency.fromString)
    }

  // Custom encoder for Price with rounding
  implicit val priceEncoder: Encoder[BigDecimal] = Encoder.instance { price =>
    val rounded = roundPrice(price)
    Json.fromBigDecimal(rounded)
  }

  // Custom encoder for Timestamp
  implicit val timestampEncoder: Encoder[String] = Encoder.instance { ts =>
    Json.fromString(ts)
  }

  implicit val responseEncoder: Encoder[GetApiResponse] =
    deriveConfiguredEncoder[GetApiResponse]

  implicit val errorResponseEncoder: Encoder[ErrorResponse] =
    deriveConfiguredEncoder[ErrorResponse]

  // Helper to round price according to requirements
  private def roundPrice(price: BigDecimal): BigDecimal = {
    if (price >= 0.1) {
      // 4 decimal places for rates >= 0.1
      price.setScale(4, BigDecimal.RoundingMode.HALF_UP)
    } else {
      // 4 significant figures for rates < 0.1
      val scale = 4 - price.precision + price.scale
      price.setScale(Math.max(scale, 0), BigDecimal.RoundingMode.HALF_UP)
    }
  }

  // Helper to format timestamp
  def formatTimestamp(timestamp: Timestamp): String = {
    timestamp.value.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
  }
}