package forex.domain

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import java.time.OffsetDateTime

class RateSpec extends AnyFlatSpec with Matchers {

  "Rate.Pair" should "be created with different currencies" in {
    val pair = Rate.Pair(Currency.USD, Currency.EUR)
    pair.from shouldBe Currency.USD
    pair.to shouldBe Currency.EUR
  }

  "Rate" should "contain pair, price, and timestamp" in {
    val pair = Rate.Pair(Currency.USD, Currency.EUR)
    val price = Price(BigDecimal("1.2345"))
    val timestamp = Timestamp(OffsetDateTime.now())

    val rate = Rate(pair, price, timestamp)

    rate.pair shouldBe pair
    rate.price shouldBe price
    rate.timestamp shouldBe timestamp
  }

  it should "handle different currency pairs" in {
    val pairs = List(
      Rate.Pair(Currency.USD, Currency.JPY),
      Rate.Pair(Currency.GBP, Currency.EUR),
      Rate.Pair(Currency.AUD, Currency.CAD)
    )

    pairs.foreach { pair =>
      pair.from should not be pair.to
    }
  }
}