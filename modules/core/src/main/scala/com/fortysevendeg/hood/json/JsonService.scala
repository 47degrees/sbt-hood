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

package com.fortysevendeg.hood.json

import java.io.File

import cats.effect.Sync
import cats.implicits._
import com.fortysevendeg.hood.model.{Benchmark, BenchmarkLoadingError, HoodError}
import com.fortysevendeg.hood.model.Benchmark._
import com.fortysevendeg.hood.utils.FileUtils
import io.circe.parser._
import com.fortysevendeg.hood.model.InvalidJson

trait JsonService[F[_]] {

  def parseBenchmark(jsonFile: File): F[Either[HoodError, List[Benchmark]]]

}

object JsonService {

  def build[F[_]: Sync]: JsonService[F] = new JsonServiceImpl[F]

  class JsonServiceImpl[F[_]](implicit S: Sync[F]) extends JsonService[F] {

    def parseBenchmark(jsonFile: File): F[Either[HoodError, List[Benchmark]]] = {

      FileUtils
        .openFile(jsonFile)
        .attempt
        .use(fileData =>
          S.pure(
            fileData
              .leftMap[HoodError](e => BenchmarkLoadingError(e.getMessage))
              .flatMap { data =>
                decode[List[Benchmark]](data.mkString)
                  .leftMap[HoodError](e => InvalidJson(e.getMessage))
              }
          )
        )
    }
  }

}
