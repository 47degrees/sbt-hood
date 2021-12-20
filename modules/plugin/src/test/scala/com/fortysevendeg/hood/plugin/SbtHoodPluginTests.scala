/*
 * Copyright 2019-2020 47 Degrees Open Source <https://www.47deg.com>
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

package com.fortysevendeg.hood.plugin

import java.io.File

import cats.data.EitherT
import cats.effect.IO
import com.fortysevendeg.hood.benchmark.BenchmarkComparisonResult
import org.typelevel.log4cats.slf4j.Slf4jLogger
import com.fortysevendeg.hood.json.JsonService
import com.fortysevendeg.hood.model.{Benchmark, HoodError}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class SbtHoodPluginTests extends AnyFlatSpec with Matchers with TestUtils {

  implicit val logger = Slf4jLogger.getLogger[IO]

  import cats.effect.unsafe.implicits.global

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
      benchmarkResultAgainstNiceDefault
    )
  }

  it should "compare two Csv benchmarks and return a warning if the current one is worse (but under threshold) with default settings" in {
    val currentFile = new File(getClass.getResource("/current_worse.csv").getPath)

    checkComparisonDefaultThreshold(
      previousFileCsv,
      currentFile,
      benchmarkResultAgainstNotSoNiceDefault
    )
  }

  it should "compare two Json benchmarks and return a warning if the current one is worse (but under threshold) with default settings" in {
    val currentFile = new File(getClass.getResource("/current_worse.json").getPath)

    checkComparisonDefaultThreshold(
      previousFileJson,
      currentFile,
      benchmarkResultAgainstNotSoNiceDefault
    )
  }

  it should "compare two Csv benchmarks and return a warning if the current one is worse (and under threshold) with default settings" in {
    val currentFile = new File(getClass.getResource("/current_really_bad.csv").getPath)

    checkComparisonDefaultThreshold(previousFileCsv, currentFile, benchmarkResultAgainstBadDefault)
  }

  it should "compare two Json benchmarks and return a warning if the current one is worse (and under threshold) with default settings" in {
    val currentFile = new File(getClass.getResource("/current_really_bad.json").getPath)

    checkComparisonDefaultThreshold(previousFileJson, currentFile, benchmarkResultAgainstBadDefault)
  }

  it should "group benchmarks for the two source files in order to produce a correct output file" in {
    val jsonService = JsonService.build[IO]
    val currentFile = new File(getClass.getResource("/current_really_bad.json").getPath)

    val grouping = (for {
      previousBenchmarks <- loadBenchmarkJson(jsonService, previousFileJson)
      currentBenchmarks  <- loadBenchmarkJson(jsonService, currentFile)
      comparisonResult <-
        TaskAlgebra
          .benchmarkTask[IO](
            previousFileJson,
            currentFile,
            "Benchmark",
            "Score",
            "Score Error (99.9%)",
            "Mode",
            "Unit",
            None,
            Map.empty,
            None,
            None,
            shouldOutputToFile = false,
            new File("output.json"),
            outputFileFormat = OutputFileFormatJson
          )
      result = TaskAlgebra.collectBenchmarks(
        previousFileJson.getName,
        currentFile.getName,
        previousBenchmarks,
        currentBenchmarks,
        comparisonResult
      )
    } yield result).value
      .unsafeRunSync()

    grouping.isRight shouldBe true
    grouping.fold(
      e => fail(s"Failed with error: $e"),
      group => {
        group.exists(_.benchmark == "test.decoding.previous") &&
        group.exists(_.benchmark == "test.parsing.previous") &&
        group.exists(_.benchmark == "test.decoding.current_really_bad") &&
        group.exists(_.benchmark == "test.parsing.current_really_bad")
      }
    )
  }

  it should "exclude benchmarks based in settings" in {
    val jsonService = JsonService.build[IO]

    val filteredBenchmarks = (for {
      previousBenchmarks <- loadBenchmarkJson(jsonService, previousFileJson)
      result = TaskAlgebra.filterBenchmarks(
        previousBenchmarks,
        None,
        Some("test.decoding")
      )
    } yield result).value
      .unsafeRunSync()

    filteredBenchmarks.isRight shouldBe true
    filteredBenchmarks.fold(
      e => fail(s"Failed with error: $e"),
      result => result.size shouldBe 1
    )
  }

  it should "include benchmarks based in settings" in {
    val jsonService = JsonService.build[IO]

    val filteredBenchmarks = (for {
      previousBenchmarks <- loadBenchmarkJson(jsonService, previousFileJson)
      result = TaskAlgebra.filterBenchmarks(
        previousBenchmarks,
        Some("test.decoding"),
        None
      )
    } yield result).value
      .unsafeRunSync()

    filteredBenchmarks.isRight shouldBe true
    filteredBenchmarks.fold(
      e => fail(s"Failed with error: $e"),
      result => result.size shouldBe 1
    )
  }

  private[this] def checkComparisonDefaultThreshold(
      previousFile: File,
      currentFile: File,
      expected: List[BenchmarkComparisonResult]
  ) = {
    val result = TaskAlgebra
      .benchmarkTask[IO](
        previousFile,
        currentFile,
        "Benchmark",
        "Score",
        "Score Error (99.9%)",
        "Mode",
        "Unit",
        None,
        Map.empty,
        None,
        None,
        shouldOutputToFile = false,
        new File("output.json"),
        outputFileFormat = OutputFileFormatJson
      )
      .value
      .unsafeRunSync()

    result.isRight shouldBe true
    result.map(resultList => resultList.sortBy(_.previous.benchmark) shouldBe expected)

    result
  }

  private[this] def loadBenchmarkJson(
      jsonService: JsonService[IO],
      file: File
  ): EitherT[IO, HoodError, Map[String, Benchmark]] =
    EitherT(jsonService.parseBenchmark(file)).map(TaskAlgebra.buildBenchmarkMap)

}
