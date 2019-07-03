package sbtHood.model

import io.circe._
import io.circe.generic.semiauto._


object Decoders {

  implicit val jsonBenchmarkdecode: Decoder[JsonBenchmark] = deriveDecoder[JsonBenchmark]
  implicit val jsonPrimaryMetricdecode: Decoder[JsonPrimaryMetric] = deriveDecoder[JsonPrimaryMetric]

}
