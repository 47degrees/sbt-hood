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

import cats.data.EitherT
import cats.effect.Sync
import com.fortysevendeg.hood.model._
import kantan.csv._
import kantan.csv.ops._
import kantan.csv.generic._
import cats.implicits._
import io.chrisdavenport.log4cats.Logger

import scala.io.Source

trait CsvService[F[_]] {

  def parseBenchmark(csvPath: File): F[Either[HoodError, List[Benchmark]]]

}

object CsvService {

  def build[F[_]: Sync: Logger]: CsvService[F] = new CsvServiceImpl[F]

  class CsvServiceImpl[F[_]](implicit S: Sync[F], L: Logger[F]) extends CsvService[F] {

    def parseBenchmark(csvPath: File): F[Either[HoodError, List[Benchmark]]] =
      (for {
        file   <- EitherT.right(S.delay(Source.fromFile(csvPath)))
        result <- parseCsvLines(file.getLines().drop(1))
        benchmarks = result.map(_.toBenchmark())
        _          = file.close()
      } yield benchmarks).value

    private[this] def parseCsvLines[F[_]](rawData: Iterator[String])(
        implicit S: Sync[F],
        L: Logger[F]): EitherT[F, HoodError, List[JmhResult]] = EitherT {
      S.delay {
        val result = rawData
          .map(_.asCsvReader[JmhResult](rfc.withoutHeader).toList)
          .toList

        result.flatten.sequence
          .leftMap[HoodError] { e =>
            L.error(s"Found error while loading CSV file: ${e.getMessage}")
            InvalidCsv(e.getMessage)
          }
      }

    }
  }

}
