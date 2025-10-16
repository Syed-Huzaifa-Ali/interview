package forex.domain

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class PriceSpec extends AnyFlatSpec with Matchers {

  "Price" should "be created from BigDecimal" in {
    val price = Price(BigDecimal("1.2345"))
    price.value shouldBe BigDecimal("1.2345")
  }

  it should "handle small decimal values" in {
    val price = Price(BigDecimal("0.0001"))
    price.value shouldBe BigDecimal("0.0001")
  }

  it should "handle large decimal values" in {
    val price = Price(BigDecimal("999999.9999"))
    price.value shouldBe BigDecimal("999999.9999")
  }
}