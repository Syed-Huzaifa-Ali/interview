package forex.domain

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import java.time.OffsetDateTime

class TimestampSpec extends AnyFlatSpec with Matchers {

  "Timestamp.now" should "create a timestamp with current time" in {
    val before = OffsetDateTime.now()
    val timestamp = Timestamp.now
    val after = OffsetDateTime.now()

    timestamp.value.isAfter(before.minusSeconds(1)) shouldBe true
    timestamp.value.isBefore(after.plusSeconds(1)) shouldBe true
  }

  "Timestamp" should "wrap OffsetDateTime" in {
    val now = OffsetDateTime.now()
    val timestamp = Timestamp(now)
    timestamp.value shouldBe now
  }

  it should "preserve the exact time" in {
    val specificTime = OffsetDateTime.parse("2024-01-15T10:30:00Z")
    val timestamp = Timestamp(specificTime)
    timestamp.value shouldBe specificTime
  }

  it should "handle different timezones" in {
    val utcTime = OffsetDateTime.parse("2024-01-15T10:30:00Z")
    val estTime = OffsetDateTime.parse("2024-01-15T05:30:00-05:00")

    val timestamp1 = Timestamp(utcTime)
    val timestamp2 = Timestamp(estTime)

    // Both represent the same instant
    timestamp1.value.toInstant shouldBe timestamp2.value.toInstant
  }

  "Multiple Timestamp.now calls" should "create different timestamps" in {
    val timestamp1 = Timestamp.now
    Thread.sleep(10) // Small delay
    val timestamp2 = Timestamp.now

    timestamp2.value.isAfter(timestamp1.value) shouldBe true
  }
}