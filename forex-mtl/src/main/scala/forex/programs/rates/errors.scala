package forex.programs.rates

import forex.services.rates.errors.{ Error => RatesServiceError }

object errors {

  sealed trait Error extends Exception {
    def message: String
  }

  object Error {
    final case class RateLookupFailed(message: String) extends Error
    final case class ServiceUnavailable(message: String) extends Error
    final case class InvalidCurrency(message: String) extends Error
  }

  def toProgramError(error: RatesServiceError): Error = error match {
    case RatesServiceError.OneFrameLookupFailed(msg) =>
      if (msg.contains("timeout") || msg.contains("Timeout")) {
        Error.ServiceUnavailable("Unable to reach external rate service. Please try again later.")
      } else {
        Error.RateLookupFailed(msg)
      }
  }
}