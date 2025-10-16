package forex.http.rates

import forex.domain._
import io.circe.syntax._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import java.time.OffsetDateTime

class ProtocolSpec extends AnyFlatSpec with Matchers {
  import Protocol._

  "GetApiResponse encoder" should "encode response correctly" in {
    val response = GetApiResponse(
      from = Currency.USD,
      to = Currency.EUR,
      price = BigDecimal("1.2345"),
      timestamp = "2024-01-01T12:00:00Z"
    )

    val json = response.asJson
    val jsonString = json.noSpaces

    jsonString should include("\"from\":\"USD\"")
    jsonString should include("\"to\":\"EUR\"")
    jsonString should include("\"price\":1.2345")
    jsonString should include("\"timestamp\":\"2024-01-01T12:00:00Z\"")
  }

  "Price rounding" should "round to 4 decimal places for values >= 0.1" in {
    val response1 = GetApiResponse(Currency.USD, Currency.EUR, BigDecimal("1.23456789"), "2024-01-01T12:00:00Z")
    val json1 = response1.asJson.noSpaces
    json1 should include("\"price\":1.2346")

    val response2 = GetApiResponse(Currency.USD, Currency.EUR, BigDecimal("0.123456"), "2024-01-01T12:00:00Z")
    val json2 = response2.asJson.noSpaces
    json2 should include("\"price\":0.1235")
  }

  it should "round to 4 significant figures for values < 0.1" in {
    val response1 = GetApiResponse(Currency.USD, Currency.EUR, BigDecimal("0.0123456"), "2024-01-01T12:00:00Z")
    val json1 = response1.asJson.noSpaces
    json1 should include("\"price\":0.01235")

    val response2 = GetApiResponse(Currency.USD, Currency.EUR, BigDecimal("0.00012345"), "2024-01-01T12:00:00Z")
    val json2 = response2.asJson.noSpaces
    json2 should include("\"price\":0.0001235")
  }

  it should "handle edge case at 0.1" in {
    val response = GetApiResponse(Currency.USD, Currency.EUR, BigDecimal("0.1"), "2024-01-01T12:00:00Z")
    val json = response.asJson.noSpaces
    json should include("\"price\":0.1")
  }

  "ErrorResponse encoder" should "encode error correctly" in {
    val error = ErrorResponse("Test error message")
    val json = error.asJson
    val jsonString = json.noSpaces

    jsonString should include("\"error\":\"Test error message\"")
  }

  "formatTimestamp" should "format timestamp in ISO format" in {
    val timestamp = Timestamp(OffsetDateTime.parse("2024-01-01T12:00:00Z"))
    val formatted = formatTimestamp(timestamp)
    formatted should include("2024-01-01")
    formatted should include("12:00:00")
  }

  it should "preserve timezone information" in {
    val timestamp = Timestamp(OffsetDateTime.parse("2024-01-01T12:00:00+05:00"))
    val formatted = formatTimestamp(timestamp)
    formatted should include("+05:00")
  }

  "Response JSON" should "use snake_case for field names" in {
    val response = GetApiResponse(
      from = Currency.USD,
      to = Currency.EUR,
      price = BigDecimal("1.2345"),
      timestamp = "2024-01-01T12:00:00Z"
    )
    val jsonString = response.asJson.noSpaces

    // Should not have camelCase
    jsonString should not include "timeStamp"
    // Should have snake_case (though timestamp is already lowercase)
    jsonString should include("\"timestamp\"")
  }
}