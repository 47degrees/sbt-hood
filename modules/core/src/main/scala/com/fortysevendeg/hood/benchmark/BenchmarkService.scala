/*
 * Copyright 2019-2023 47 Degrees Open Source <https://www.47deg.com>
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

package com.fortysevendeg.hood.benchmark

import com.fortysevendeg.hood.model.Benchmark
import scala.math.abs
import com.lightbend.emoji.ShortCodes.Implicits._
import com.lightbend.emoji.ShortCodes.Defaults._

sealed trait BenchmarkComparisonStatus extends Product with Serializable
case object OK                         extends BenchmarkComparisonStatus
case object Warning                    extends BenchmarkComparisonStatus
case object Error                      extends BenchmarkComparisonStatus

final case class BenchmarkComparisonResult(
    previous: Benchmark,
    current: Option[Benchmark],
    result: BenchmarkComparisonStatus,
    threshold: Double
) {
  def icon: String = {
    (result match {
      case OK      => "heavy_check_mark"
      case Warning => "warning"
      case _       => "red_circle"
    }).emoji.toString()
  }
}

object BenchmarkService {

  def compare(
      currentBenchmark: Benchmark,
      previousBenchmark: Benchmark,
      threshold: Double
  ): BenchmarkComparisonResult = {

    val status =
      currentBenchmark.primaryMetric.score - previousBenchmark.primaryMetric.score match {
        case comp if comp > 0               => OK
        case comp if abs(comp) <= threshold => Warning
        case _                              => Error
      }

    BenchmarkComparisonResult(
      previousBenchmark,
      Some(currentBenchmark),
      status,
      threshold
    )
  }

}
