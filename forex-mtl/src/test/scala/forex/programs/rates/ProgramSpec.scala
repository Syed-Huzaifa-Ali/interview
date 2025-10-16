package forex.programs.rates

import cats.Id
import forex.domain.{ Currency, Price, Rate, Timestamp }
import forex.services.RatesService
import forex.services.rates.errors.{ Error => ServiceError }
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import java.time.OffsetDateTime

class ProgramSpec extends AnyFlatSpec with Matchers {

  class MockRatesService(response: ServiceError Either Rate) extends RatesService[Id] {
    override def get(pair: Rate.Pair): Id[ServiceError Either Rate] = response
  }

  "Program.get" should "return rate when service succeeds" in {
    val pair = Rate.Pair(Currency.USD, Currency.EUR)
    val rate = Rate(pair, Price(BigDecimal("1.23")), Timestamp(OffsetDateTime.now()))
    val service = new MockRatesService(Right(rate))
    val program = Program[Id](service)

    val request = Protocol.GetRatesRequest(Currency.USD, Currency.EUR)
    val result = program.get(request)

    result shouldBe Right(rate)
  }

  it should "return error when service fails" in {
    val service = new MockRatesService(Left(ServiceError.OneFrameLookupFailed("Service down")))
    val program = Program[Id](service)

    val request = Protocol.GetRatesRequest(Currency.USD, Currency.EUR)
    val result = program.get(request)

    result.isLeft shouldBe true
  }

  it should "map service error to program error" in {
    val service = new MockRatesService(Left(ServiceError.OneFrameLookupFailed("Timeout")))
    val program = Program[Id](service)

    val request = Protocol.GetRatesRequest(Currency.USD, Currency.EUR)
    val result = program.get(request)

    result match {
      case Left(error) => error shouldBe a[errors.Error]
      case Right(_)    => fail("Expected error")
    }
  }

  it should "handle different currency pairs" in {
    val pairs = List(
      (Currency.USD, Currency.EUR),
      (Currency.GBP, Currency.JPY),
      (Currency.AUD, Currency.CAD)
    )

    pairs.foreach { case (from, to) =>
      val pair = Rate.Pair(from, to)
      val rate = Rate(pair, Price(BigDecimal("1.5")), Timestamp(OffsetDateTime.now()))
      val service = new MockRatesService(Right(rate))
      val program = Program[Id](service)

      val request = Protocol.GetRatesRequest(from, to)
      val result = program.get(request)

      result shouldBe Right(rate)
    }
  }

  it should "preserve rate details from service" in {
    val pair = Rate.Pair(Currency.EUR, Currency.GBP)
    val price = Price(BigDecimal("0.8765"))
    val timestamp = Timestamp(OffsetDateTime.parse("2024-01-15T10:30:00Z"))
    val rate = Rate(pair, price, timestamp)

    val service = new MockRatesService(Right(rate))
    val program = Program[Id](service)

    val request = Protocol.GetRatesRequest(Currency.EUR, Currency.GBP)
    val result = program.get(request)

    result match {
      case Right(r) =>
        r.pair shouldBe pair
        r.price shouldBe price
        r.timestamp shouldBe timestamp
      case Left(_) => fail("Expected success")
    }
  }
}