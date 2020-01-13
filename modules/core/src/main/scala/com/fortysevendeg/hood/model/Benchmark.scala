/*
 * Copyright 2019-2020 47 Degrees, LLC. <http://www.47deg.com>
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

final case class Benchmark(
    benchmark: String,
    mode: String,
    primaryMetric: PrimaryMetric
)

object Benchmark {
  implicit val benchmarkDecoder: Decoder[Benchmark] = deriveDecoder[Benchmark]
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
