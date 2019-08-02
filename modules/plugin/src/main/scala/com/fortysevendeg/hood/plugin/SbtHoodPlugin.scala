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

package com.fortysevendeg.hood.plugin

import java.io.File

import cats.data.EitherT
import cats.effect.{IO, Sync}
import com.fortysevendeg.hood.benchmark.{BenchmarkComparisonResult, BenchmarkService}
import com.fortysevendeg.hood.csv.CsvService
import com.fortysevendeg.hood.model.{Benchmark, HoodError}
import sbt.{AutoPlugin, Def}
import io.chrisdavenport.log4cats.Logger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger

object SbtHoodPlugin extends AutoPlugin with SbtHoodDefaultSettings {

  override def projectSettings: Seq[Def.Setting[_]] = defaultSettings

  def benchmarkTask(
      previousPath: File,
      currentPath: File,
      keyColumnName: String,
      compareColumnName: String,
      thresholdColumnName: String,
      modeColumnName: String,
      unitsColumnName: String,
      generalThreshold: Option[Double],
      benchmarkThreshold: Map[String, Double]): EitherT[
    IO,
    HoodError,
    List[BenchmarkComparisonResult]] = {

    implicit val logger = Slf4jLogger.getLogger[IO]
    val csvService      = CsvService.build[IO]

    val parseFunction: File => IO[Either[HoodError, List[Benchmark]]] = csvService.parseBenchmark(
      keyColumnName,
      modeColumnName,
      compareColumnName,
      thresholdColumnName,
      unitsColumnName)

    for {
      previousBenchmarks <- EitherT(parseFunction(previousPath)).map(buildBenchmarkMap)
      currentBenchmarks  <- EitherT(parseFunction(currentPath)).map(buildBenchmarkMap)
      result <- EitherT.right[HoodError](
        compareBenchmarks[IO](
          currentBenchmarks,
          previousBenchmarks,
          generalThreshold,
          benchmarkThreshold))
    } yield result

  }

  private[this] def buildBenchmarkMap(benchmarks: List[Benchmark]): Map[String, Benchmark] =
    benchmarks.map(b => (b.benchmark, b)).toMap

  private[this] def compareBenchmarks[F[_]](
      currentBenchmarks: Map[String, Benchmark],
      previousBenchmarks: Map[String, Benchmark],
      generalThreshold: Option[Double],
      thresholdMap: Map[String, Double])(
      implicit L: Logger[F],
      S: Sync[F]): F[List[BenchmarkComparisonResult]] = {

    S.delay(previousBenchmarks.keys.flatMap { benchmarkKey =>
      val currentBench = currentBenchmarks.get(benchmarkKey)

      if (currentBench.isEmpty) {
        L.error(
          s"Benchmark $benchmarkKey existing in previous benchmarks is missing from current ones.")
      }

      for {
        previous <- previousBenchmarks.get(benchmarkKey)
        current  <- currentBench
        threshold = thresholdMap.getOrElse(benchmarkKey, previous.primaryMetric.scoreError)
      } yield BenchmarkService.compare(current, previous, threshold)
    }.toList)

  }

}
