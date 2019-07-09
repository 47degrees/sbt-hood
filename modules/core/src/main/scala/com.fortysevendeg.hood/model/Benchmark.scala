/*
 * Copyright 2019 47 Degrees, LLC. <http://www.47deg.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.fortysevendeg.hood.model

import io.circe._
import io.circe.generic.semiauto._
import cats.syntax.either._

sealed trait BenchmarkMode
case object Throughput     extends BenchmarkMode
case object AverageTime    extends BenchmarkMode
case object SampleTime     extends BenchmarkMode
case object SingleShotTime extends BenchmarkMode
case object All            extends BenchmarkMode

object BenchmarkMode {
  implicit val modeEncoder: Encoder[BenchmarkMode] = new Encoder[BenchmarkMode] {
    final def apply(mode: BenchmarkMode): Json =
      Json.fromString(mode match {
        case Throughput     => "throughput"
        case AverageTime    => "averageTime"
        case SampleTime     => "sampleTime"
        case SingleShotTime => "singleShotTime"
        case All            => "all"
      })
  }

  implicit val modeDecoder: Decoder[BenchmarkMode] = Decoder.decodeString.emap {
    case "throughput"     => Either.right(Throughput)
    case "averageTime"    => Either.right(AverageTime)
    case "sampleTime"     => Either.right(SampleTime)
    case "singleShotTime" => Either.right(SingleShotTime)
    case "all"            => Either.right(All)
    case _                => Either.left("Invalid benchmark mode")
  }
}

final case class Benchmark(
    benchmark: String,
    mode: BenchmarkMode,
    primaryMetric: PrimaryMetric
)

object Benchmark {
  implicit val benchmarkDecode: Decoder[Benchmark]  = deriveDecoder[Benchmark]
  implicit val benchmarkEncoder: Encoder[Benchmark] = deriveEncoder[Benchmark]
}

final case class PrimaryMetric(
    score: Double,
    scoreError: Double,
    scoreUnit: String,
    rawData: List[List[Double]]
)

object PrimaryMetric {
  implicit val primaryMetricDecode: Decoder[PrimaryMetric] =
    deriveDecoder[PrimaryMetric]
  implicit val primaryMetricEncoder: Encoder[PrimaryMetric] =
    deriveEncoder[PrimaryMetric]
}
