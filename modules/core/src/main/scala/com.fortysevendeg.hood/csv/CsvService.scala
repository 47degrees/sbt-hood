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

package com.fortysevendeg.hood.csv

import java.io.File

import cats.effect.{Resource, Sync}
import com.fortysevendeg.hood.model._
import kantan.csv._
import kantan.csv.ops._
import kantan.csv.generic._
import cats.implicits._
import io.chrisdavenport.log4cats.Logger

import scala.io.{BufferedSource, Source}

trait CsvService[F[_]] {

  def parseBenchmark(
      keyCol: String,
      modeCol: String,
      compareCol: String,
      thresholdCol: String,
      unitsCol: String)(csvFile: File): F[Either[HoodError, List[Benchmark]]]

}

object CsvService {

  def build[F[_]: Sync: Logger]: CsvService[F] = new CsvServiceImpl[F]

  class CsvServiceImpl[F[_]](implicit S: Sync[F], L: Logger[F]) extends CsvService[F] {

    def parseBenchmark(
        keyCol: String,
        modeCol: String,
        compareCol: String,
        thresholdCol: String,
        unitsCol: String)(csvFile: File): F[Either[HoodError, List[Benchmark]]] =
      openFile(csvFile).attempt
        .use(fileData =>
          S.pure(for {
            data <- fileData
              .leftMap[HoodError](e => BenchmarkLoadingError(e.getMessage))
            result <- parseCsvLinesHeaders(
              data.mkString,
              keyCol,
              modeCol,
              compareCol,
              thresholdCol,
              unitsCol)
          } yield result))

    private[this] def openFile(file: File): Resource[F, BufferedSource] =
      Resource(S.delay {
        val fileBuffer = Source.fromFile(file)
        (fileBuffer, S.delay(fileBuffer.close()))
      })

    private[this] def parseCsvLinesHeaders(
        rawData: String,
        keyCol: String,
        modeCol: String,
        compareCol: String,
        thresholdCol: String,
        unitsCol: String
    ): Either[HoodError, List[Benchmark]] = {

      implicit val decoder: HeaderDecoder[JmhResult] = HeaderDecoder.decoder(
        keyCol,
        modeCol,
        "Threads",
        "Samples",
        compareCol,
        thresholdCol,
        unitsCol)(JmhResult.apply _)

      rawData
        .asCsvReader[JmhResult](rfc.withHeader)
        .map(
          result =>
            result
              .map(_.toBenchmark())
              .leftMap[HoodError] { e =>
                InvalidCsv(e.getMessage)
            })
        .toList
        .sequence
    }

  }

}
