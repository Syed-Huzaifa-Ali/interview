package forex.programs.rates

import forex.services.rates.errors.{ Error => ServiceError }
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class ErrorsSpec extends AnyFlatSpec with Matchers {

  "toProgramError" should "map timeout error to ServiceUnavailable" in {
    val serviceError = ServiceError.OneFrameLookupFailed("Request timeout after 500ms")
    val programError = errors.toProgramError(serviceError)

    programError shouldBe a[errors.Error.ServiceUnavailable]
    programError.message should include("Unable to reach external rate service")
  }

  it should "map Timeout error to ServiceUnavailable" in {
    val serviceError = ServiceError.OneFrameLookupFailed("Timeout occurred")
    val programError = errors.toProgramError(serviceError)

    programError shouldBe a[errors.Error.ServiceUnavailable]
  }

  it should "map lowercase timeout error to ServiceUnavailable" in {
    val serviceError = ServiceError.OneFrameLookupFailed("connection timeout")
    val programError = errors.toProgramError(serviceError)

    programError shouldBe a[errors.Error.ServiceUnavailable]
  }

  it should "map other errors to RateLookupFailed" in {
    val serviceError = ServiceError.OneFrameLookupFailed("Generic error")
    val programError = errors.toProgramError(serviceError)

    programError shouldBe a[errors.Error.RateLookupFailed]
    programError.message shouldBe "Generic error"
  }

  "Error" should "have proper message field" in {
    val error1 = errors.Error.RateLookupFailed("Test message 1")
    error1.message shouldBe "Test message 1"

    val error2 = errors.Error.ServiceUnavailable("Test message 2")
    error2.message shouldBe "Test message 2"

    val error3 = errors.Error.InvalidCurrency("Test message 3")
    error3.message shouldBe "Test message 3"
  }

  "Error" should "extend Exception" in {
    val error = errors.Error.RateLookupFailed("test")
    error shouldBe an[Exception]
  }

  "Error types" should "be distinguishable" in {
    val error1: errors.Error = errors.Error.RateLookupFailed("msg1")
    val error2: errors.Error = errors.Error.ServiceUnavailable("msg2")
    val error3: errors.Error = errors.Error.InvalidCurrency("msg3")

    error1 should not be error2
    error2 should not be error3
    error1 should not be error3
  }
}