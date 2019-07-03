package sbtHood.model

import io.circe._
import io.circe.generic.semiauto._


object Encoders {

  implicit val jsonBenchmarkEncoder: Encoder[JsonBenchmark] = deriveEncoder[JsonBenchmark]
  implicit val jsonPrimaryMetricEncoder: Encoder[JsonPrimaryMetric] = deriveEncoder[JsonPrimaryMetric]
}
