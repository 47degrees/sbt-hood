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

package com.fortysevendeg.hood.csv

import java.io.File

import cats.effect.Sync
import cats.syntax.either._
import com.fortysevendeg.hood.model._
import kantan.csv._
import kantan.csv.ops._
import kantan.csv.generic._
import cats.implicits._
import com.fortysevendeg.hood.utils.FileUtils

final case class BenchmarkColumns(
    keyCol: String,
    modeCol: String,
    compareCol: String,
    thresholdCol: String,
    unitsCol: String
)

trait CsvService[F[_]] {

  def parseBenchmark(
      columns: BenchmarkColumns,
      csvFile: File
  ): F[Either[HoodError, List[Benchmark]]]

}

object CsvService {

  def build[F[_]: Sync]: CsvService[F] = new CsvServiceImpl[F]

  class CsvServiceImpl[F[_]](implicit S: Sync[F]) extends CsvService[F] {

    def parseBenchmark(
        columns: BenchmarkColumns,
        csvFile: File
    ): F[Either[HoodError, List[Benchmark]]] =
      FileUtils
        .openFile(csvFile)
        .attempt
        .use(fileData =>
          S.pure(for {
            data <-
              fileData
                .leftMap[HoodError](e => BenchmarkLoadingError(e.getMessage))
            result <- parseCsvLinesHeaders(data.mkString, columns)
          } yield result)
        )

    private[this] def parseCsvLinesHeaders(
        rawData: String,
        columns: BenchmarkColumns
    ): Either[HoodError, List[Benchmark]] = {

      implicit val decoder: HeaderDecoder[JmhResult] = HeaderDecoder.decoder(
        columns.keyCol,
        columns.modeCol,
        columns.compareCol,
        columns.thresholdCol,
        columns.unitsCol
      )(JmhResult.apply _)

      rawData
        .asCsvReader[JmhResult](rfc.withHeader)
        .map(result =>
          result
            .map(_.toBenchmark())
            .leftMap[HoodError](e => InvalidCsv(e.getMessage))
        )
        .toList
        .sequence
    }

  }

}
