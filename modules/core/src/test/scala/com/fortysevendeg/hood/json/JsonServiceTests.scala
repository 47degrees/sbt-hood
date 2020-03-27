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

package com.fortysevendeg.hood.json

import java.io.File
import cats.implicits._
import cats.effect.IO
import com.fortysevendeg.hood.TestValues.{badBenchmark, mehBenchmark}
import com.fortysevendeg.hood.model.InvalidJson
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class JsonServiceTests extends AnyFlatSpec with Matchers {

  implicit val logger = Slf4jLogger.getLogger[IO]

  "JsonService" should "parse a correct json file" in {
    val dataFile = new File(getClass.getResource("/jmh.json").getPath)

    val result = JsonService
      .build[IO]
      .parseBenchmark(dataFile)
      .unsafeRunSync()

    result.isRight shouldBe true
    result.map(list => list shouldBe (List(mehBenchmark, badBenchmark)))
  }

  it should "return an error when loading an invalid json file" in {
    val dataFile = new File(getClass.getResource("/invalid_jmh.json").getPath)

    val result = JsonService
      .build[IO]
      .parseBenchmark(dataFile)
      .unsafeRunSync()

    result.isLeft shouldBe true
    result.leftMap(_.isInstanceOf[InvalidJson] shouldBe true)
  }

}
