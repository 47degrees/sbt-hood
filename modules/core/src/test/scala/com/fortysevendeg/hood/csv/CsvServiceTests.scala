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

package com.fortysevendeg.hood.csv

import java.io.File

import cats.effect.IO
import cats.syntax.either._
import org.scalatest.{FlatSpec, Matchers}
import com.fortysevendeg.hood.TestValues._
import com.fortysevendeg.hood.model.InvalidCsv
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger

class CsvServiceTests extends FlatSpec with Matchers {

  implicit val logger = Slf4jLogger.getLogger[IO]

  "CsvService" should "parse a correct CSV file" in {
    val dataFile = new File(getClass.getResource("/jmh.csv").getPath)

    val result = CsvService
      .build[IO]
      .parseBenchmark(
        BenchmarkColumns("Benchmark", "Mode", "Score", "Score Error (99.9%)", "Unit"),
        dataFile
      )
      .unsafeRunSync()

    result.isRight shouldBe true
    result.map { list =>
      list shouldBe (List(mehBenchmark, badBenchmark))
    }
  }

  it should "return an error when loading an invalid CSV file" in {
    val dataFile = new File(getClass.getResource("/invalid_jmh.csv").getPath)

    val result = CsvService
      .build[IO]
      .parseBenchmark(
        BenchmarkColumns("Benchmark", "Mode", "Score", "Score Error (99.9%)", "Unit"),
        dataFile
      )
      .unsafeRunSync()

    result.isLeft shouldBe true
    result.leftMap(_.isInstanceOf[InvalidCsv] shouldBe true)
  }

}
