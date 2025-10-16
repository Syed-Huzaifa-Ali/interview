package forex.config

import scala.concurrent.duration.FiniteDuration

case class ApplicationConfig(
                              http: HttpConfig,
                              auth: AuthConfig,
                              oneFrame: OneFrameConfig,
                              redis: RedisConfig
                            )

case class HttpConfig(
                       host: String,
                       port: Int,
                       timeout: FiniteDuration
                     )

case class AuthConfig(
                       token: String
                     )

case class OneFrameConfig(
                           baseUrl: String,
                           token: String,
                           requestTimeout: FiniteDuration,
                           connectTimeout: FiniteDuration
                         )

case class RedisConfig(
                        uri: String,
                        cacheTtl: FiniteDuration,
                        refreshInterval: FiniteDuration
                      )