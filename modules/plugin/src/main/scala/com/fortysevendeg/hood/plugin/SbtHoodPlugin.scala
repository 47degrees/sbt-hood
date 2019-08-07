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
import cats.effect.{Console, IO, Sync}
import com.fortysevendeg.hood.benchmark.{BenchmarkComparisonResult, BenchmarkService, Warning}
import com.fortysevendeg.hood.csv.CsvService
import com.fortysevendeg.hood.model.{Benchmark, HoodError}
import sbt.{AutoPlugin, Def, PluginTrigger}
import io.chrisdavenport.log4cats.Logger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import cats.effect.Console.implicits._

object SbtHoodPlugin extends AutoPlugin with SbtHoodDefaultSettings with SbtHoodKeys {

  override def projectSettings: Seq[Def.Setting[_]] = defaultSettings
  override val trigger: PluginTrigger               = noTrigger

  compareBenchmarks := {

    implicit val logger = Slf4jLogger.getLogger[IO]

    benchmarkTask(
      previousBenchmarkPath.value,
      currentBenchmarkPath.value,
      keyColumnName.value,
      compareColumnName.value,
      thresholdColumnName.value,
      modeColumnName.value,
      unitsColumnName.value,
      generalThreshold.value,
      benchmarkThreshold.value
    ).value
      .unsafeRunSync()

    ()
  }

  def benchmarkTask[F[_]](
      previousPath: File,
      currentPath: File,
      keyColumnName: String,
      compareColumnName: String,
      thresholdColumnName: String,
      modeColumnName: String,
      unitsColumnName: String,
      generalThreshold: Option[Double],
      benchmarkThreshold: Map[String, Double])(
      implicit L: Logger[F],
      S: Sync[F],
      C: Console[F]): EitherT[F, HoodError, List[BenchmarkComparisonResult]] = {

    val csvService = CsvService.build[F]

    val parseFunction: File => F[Either[HoodError, List[Benchmark]]] = csvService.parseBenchmark(
      keyColumnName,
      modeColumnName,
      compareColumnName,
      thresholdColumnName,
      unitsColumnName)

    for {
      previousBenchmarks <- EitherT(parseFunction(previousPath)).map(buildBenchmarkMap)
      currentBenchmarks  <- EitherT(parseFunction(currentPath)).map(buildBenchmarkMap)
      result <- EitherT.right[HoodError](
        performBenchmarkComparison[F](
          currentBenchmarks,
          previousBenchmarks,
          generalThreshold,
          benchmarkThreshold))
      outputMessage = benchmarkOutput(result, previousPath.getName, currentPath.getName)
      _ <- EitherT.right(C.putStrLn(outputMessage))
    } yield result

  }

  private[this] def buildBenchmarkMap(benchmarks: List[Benchmark]): Map[String, Benchmark] =
    benchmarks.map(b => (b.benchmark, b)).toMap

  private[this] def performBenchmarkComparison[F[_]](
      currentBenchmarks: Map[String, Benchmark],
      previousBenchmarks: Map[String, Benchmark],
      generalThreshold: Option[Double],
      thresholdMap: Map[String, Double])(
      implicit L: Logger[F],
      S: Sync[F]): F[List[BenchmarkComparisonResult]] =
    S.delay(previousBenchmarks.map {
      case (benchmarkKey, previous) =>
        val threshold =
          thresholdMap
            .get(benchmarkKey)
            .fold(generalThreshold.getOrElse(previous.primaryMetric.scoreError))(identity)

        currentBenchmarks
          .get(benchmarkKey)
          .fold({
            L.error(
              s"Benchmark $benchmarkKey existing in previous benchmarks is missing from current ones.")
            BenchmarkComparisonResult(previous, None, Warning, threshold)
          })(current => BenchmarkService.compare(current, previous, threshold))
    }.toList)

  private[this] def benchmarkOutput(
      benchmarks: List[BenchmarkComparisonResult],
      previousFile: String,
      currentFile: String): String = {
    def outputComparisonResult(result: BenchmarkComparisonResult): String =
      s"""
         |${result.icon()} ${result.previous.benchmark} (Threshold: ${result.threshold})
         |
      |Benchmark|Value
         |$previousFile|${result.previous.primaryMetric.score.toString}
         |$currentFile|${result.current.map(_.primaryMetric.score.toString).getOrElse("N/A")}
    """.stripMargin

    benchmarks.map(b => outputComparisonResult(b)).mkString("\n\n")
  }
}
