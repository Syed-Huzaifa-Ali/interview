import sbt._

object Dependencies {
  object Versions {
    val cats                = "2.6.1"
    val catsEffect          = "2.5.5"
    val fs2                 = "2.5.11"
    val http4s              = "0.21.34"
    val circe               = "0.14.1"
    val circeGenericExtras  = "0.14.1"
    val pureConfig          = "0.17.1"
    val logback             = "1.2.11"
    val kindProjector       = "0.13.2"
    val redis4cats          = "0.14.0"

    val scalaTest           = "3.2.15"
    val scalaCheck          = "1.17.0"
    val catsScalaCheck      = "0.3.2"
  }

  object Libraries {
    def circe(artifact: String): ModuleID = {
      if (artifact == "circe-generic-extras") {
        "io.circe" %% artifact % Versions.circeGenericExtras
      } else {
        "io.circe" %% artifact % Versions.circe
      }
    }
    def http4s(artifact: String): ModuleID = "org.http4s" %% artifact % Versions.http4s
    def redis4cats(artifact: String): ModuleID = "dev.profunktor" %% artifact % Versions.redis4cats

    lazy val cats                = "org.typelevel"         %% "cats-core"                  % Versions.cats
    lazy val catsEffect          = "org.typelevel"         %% "cats-effect"                % Versions.catsEffect
    lazy val fs2                 = "co.fs2"                %% "fs2-core"                   % Versions.fs2

    lazy val http4sDsl           = http4s("http4s-dsl")
    lazy val http4sServer        = http4s("http4s-blaze-server")
    lazy val http4sClient        = http4s("http4s-blaze-client")
    lazy val http4sCirce         = http4s("http4s-circe")

    lazy val circeCore           = circe("circe-core")
    lazy val circeGeneric        = circe("circe-generic")
    lazy val circeGenericExt     = circe("circe-generic-extras")
    lazy val circeParser         = circe("circe-parser")

    lazy val redis4catsEffects   = redis4cats("redis4cats-effects")
    lazy val redis4catsLog4cats  = redis4cats("redis4cats-log4cats")

    lazy val pureConfig          = "com.github.pureconfig" %% "pureconfig"                % Versions.pureConfig

    lazy val kindProjector       = "org.typelevel"         %% "kind-projector"            % Versions.kindProjector cross CrossVersion.full
    lazy val logback             = "ch.qos.logback"        %  "logback-classic"           % Versions.logback

    lazy val scalaTest           = "org.scalatest"         %% "scalatest"                 % Versions.scalaTest
    lazy val scalaCheck          = "org.scalacheck"        %% "scalacheck"                % Versions.scalaCheck
    lazy val catsScalaCheck      = "io.chrisdavenport"     %% "cats-scalacheck"           % Versions.catsScalaCheck
  }
}