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

package com.fortysevendeg.hood.benchmark

import com.fortysevendeg.hood.TestValues._
import org.scalatest.{FlatSpec, Matchers}

class BenchmarkServiceTests extends FlatSpec with Matchers {

  "BenchmarkService" should "return an OK status if current benchmark exceeds the previous" in {
    val benchmarkResult = BenchmarkService
      .compare(
        excellentBenchmark,
        mehBenchmark,
        threshold = 5
      )

    benchmarkResult.result shouldBe OK
  }

  it should "return a Warning status if current benchmark is just within threshold" in {
    val benchmarkResult = BenchmarkService
      .compare(
        mehBenchmark,
        excellentBenchmark,
        threshold = 5
      )

    benchmarkResult.result shouldBe Warning
  }

  it should "return an Error status if current benchmark is below threshold" in {
    val benchmarkResult = BenchmarkService
      .compare(
        badBenchmark,
        excellentBenchmark,
        threshold = 5
      )

    benchmarkResult.result shouldBe Error
  }

}
