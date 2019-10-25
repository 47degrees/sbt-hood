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

import java.io.File

import cats.effect.IO
import com.fortysevendeg.hood.benchmark.BenchmarkComparisonResult
import com.fortysevendeg.hood.plugin.SbtHoodPlugin
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import org.scalatest.{FlatSpec, Matchers}
import cats.effect.Console.implicits._

class SbtHoodPluginTests extends FlatSpec with Matchers with TestUtils {

  implicit val logger = Slf4jLogger.getLogger[IO]

  val previousFileCsv  = new File(getClass.getResource("/previous.csv").getPath)
  val previousFileJson = new File(getClass.getResource("/previous.json").getPath)

  "SbtHoodPlugin" should "compare two CSV benchmarks and return a valid result if the current one is better with default settings" in {
    val currentFile = new File(getClass.getResource("/current_better.csv").getPath)

    checkComparisonDefaultThreshold(previousFileCsv, currentFile, benchmarkResultAgainstNiceDefault)
  }

  it should "compare two Json benchmarks and return a valid result if the current one is better with default settings" in {
    val currentFile = new File(getClass.getResource("/current_better.json").getPath)

    checkComparisonDefaultThreshold(
      previousFileJson,
      currentFile,
      benchmarkResultAgainstNiceDefault)
  }

  it should "compare two Csv benchmarks and return a warning if the current one is worse (but under threshold) with default settings" in {
    val currentFile = new File(getClass.getResource("/current_worse.csv").getPath)

    checkComparisonDefaultThreshold(
      previousFileCsv,
      currentFile,
      benchmarkResultAgainstNotSoNiceDefault)
  }

  it should "compare two Json benchmarks and return a warning if the current one is worse (but under threshold) with default settings" in {
    val currentFile = new File(getClass.getResource("/current_worse.json").getPath)

    checkComparisonDefaultThreshold(
      previousFileJson,
      currentFile,
      benchmarkResultAgainstNotSoNiceDefault)
  }

  it should "compare two Csv benchmarks and return a warning if the current one is worse (and under threshold) with default settings" in {
    val currentFile = new File(getClass.getResource("/current_really_bad.csv").getPath)

    checkComparisonDefaultThreshold(previousFileCsv, currentFile, benchmarkResultAgainstBadDefault)
  }

  it should "compare two Json benchmarks and return a warning if the current one is worse (and under threshold) with default settings" in {
    val currentFile = new File(getClass.getResource("/current_really_bad.json").getPath)

    checkComparisonDefaultThreshold(previousFileJson, currentFile, benchmarkResultAgainstBadDefault)
  }

  private[this] def checkComparisonDefaultThreshold(
      previousFile: File,
      currentFile: File,
      expected: List[BenchmarkComparisonResult]) = {
    val result = SbtHoodPlugin
      .benchmarkTask(
        previousFile,
        currentFile,
        "Benchmark",
        "Score",
        "Score Error (99.9%)",
        "Mode",
        "Unit",
        None,
        Map.empty)
      .value
      .unsafeRunSync()

    result.isRight shouldBe true
    result.map { resultList =>
      resultList.sortBy(_.previous.benchmark) shouldBe expected
    }
  }

}
