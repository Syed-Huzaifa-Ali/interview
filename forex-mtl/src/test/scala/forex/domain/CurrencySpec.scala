package forex.domain

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class CurrencySpec extends AnyFlatSpec with Matchers {

  "Currency.fromString" should "parse valid currency codes" in {
    Currency.fromString("USD") shouldBe Currency.USD
    Currency.fromString("EUR") shouldBe Currency.EUR
    Currency.fromString("GBP") shouldBe Currency.GBP
    Currency.fromString("JPY") shouldBe Currency.JPY
    Currency.fromString("AUD") shouldBe Currency.AUD
    Currency.fromString("CAD") shouldBe Currency.CAD
    Currency.fromString("CHF") shouldBe Currency.CHF
    Currency.fromString("NZD") shouldBe Currency.NZD
    Currency.fromString("SGD") shouldBe Currency.SGD
  }

  it should "parse lowercase currency codes" in {
    Currency.fromString("usd") shouldBe Currency.USD
    Currency.fromString("eur") shouldBe Currency.EUR
  }

  it should "parse mixed case currency codes" in {
    Currency.fromString("UsD") shouldBe Currency.USD
    Currency.fromString("eUr") shouldBe Currency.EUR
  }

  "Currency.show" should "display currency as uppercase string" in {
    Currency.show.show(Currency.USD) shouldBe "USD"
    Currency.show.show(Currency.EUR) shouldBe "EUR"
    Currency.show.show(Currency.GBP) shouldBe "GBP"
    Currency.show.show(Currency.JPY) shouldBe "JPY"
  }
}