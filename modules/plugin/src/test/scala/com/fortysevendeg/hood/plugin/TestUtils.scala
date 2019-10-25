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

package scala.com.fortysevendeg.hood.plugin

import com.fortysevendeg.hood.benchmark._
import com.fortysevendeg.hood.model.{Benchmark, PrimaryMetric}

trait TestUtils {

  val prevBenchmarkDecoding =
    Benchmark("test.decoding", "thrpt", PrimaryMetric(5.0, 3.0, "love/s", List.empty))
  val prevBenchmarkParsing =
    Benchmark("test.parsing", "thrpt", PrimaryMetric(6.0, 3.0, "love/s", List.empty))
  val niceBenchmarkDecoding =
    Benchmark("test.decoding", "thrpt", PrimaryMetric(6.0, 0.1, "love/s", List.empty))
  val niceBenchmarkParsing =
    Benchmark("test.parsing", "thrpt", PrimaryMetric(7.0, 0.1, "love/s", List.empty))
  val notSoNiceBenchmarkDecoding =
    Benchmark("test.decoding", "thrpt", PrimaryMetric(3.0, 0.1, "love/s", List.empty))
  val notSoNiceBenchmarkParsing =
    Benchmark("test.parsing", "thrpt", PrimaryMetric(4.0, 0.1, "love/s", List.empty))
  val badBenchmarkDecoding =
    Benchmark("test.decoding", "thrpt", PrimaryMetric(1.0, 0.1, "love/s", List.empty))
  val badBenchmarkParsing =
    Benchmark("test.parsing", "thrpt", PrimaryMetric(0.5, 0.1, "love/s", List.empty))

  val benchmarkResultAgainstNiceDefault = List(
    BenchmarkComparisonResult(prevBenchmarkDecoding, Some(niceBenchmarkDecoding), OK, 3.0),
    BenchmarkComparisonResult(prevBenchmarkParsing, Some(niceBenchmarkParsing), OK, 3.0)
  )

  val benchmarkResultAgainstNotSoNiceDefault = List(
    BenchmarkComparisonResult(
      prevBenchmarkDecoding,
      Some(notSoNiceBenchmarkDecoding),
      Warning,
      3.0),
    BenchmarkComparisonResult(prevBenchmarkParsing, Some(notSoNiceBenchmarkParsing), Warning, 3.0)
  )

  val benchmarkResultAgainstBadDefault = List(
    BenchmarkComparisonResult(prevBenchmarkDecoding, Some(badBenchmarkDecoding), Error, 3.0),
    BenchmarkComparisonResult(prevBenchmarkParsing, Some(badBenchmarkParsing), Error, 3.0)
  )

}
