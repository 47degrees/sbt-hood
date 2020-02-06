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

package com.fortysevendeg.hood.plugin

import java.io.File

import cats.data.{EitherT, NonEmptyChain}
import cats.implicits._
import cats.effect.{Console, IO, Sync}
import com.fortysevendeg.hood.benchmark.{BenchmarkComparisonResult, BenchmarkService, Warning}
import com.fortysevendeg.hood.csv.{BenchmarkColumns, CsvService}
import com.fortysevendeg.hood.model._
import sbt.{AutoPlugin, Def, PluginTrigger, Task}
import io.chrisdavenport.log4cats.Logger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import io.circe.syntax._
import cats.effect.Console.implicits._
import com.fortysevendeg.hood.github._
import com.fortysevendeg.hood.benchmark.Error
import com.fortysevendeg.hood.json.JsonService
import com.fortysevendeg.hood.utils._
import github4s.GithubResponses.GHException
import Benchmark._
import com.lightbend.emoji.ShortCodes.Implicits._
import com.lightbend.emoji.ShortCodes.Defaults._

object SbtHoodPlugin extends AutoPlugin with SbtHoodDefaultSettings with SbtHoodKeys {

  sealed trait OutputFileFormat
  case object OutputFileFormatMd   extends OutputFileFormat
  case object OutputFileFormatJson extends OutputFileFormat

  private[this] def parseOutputFormat(source: String): OutputFileFormat =
    source.toLowerCase match {
      case "json" => OutputFileFormatJson
      case _      => OutputFileFormatMd
    }

  override def projectSettings: Seq[Def.Setting[_]] = defaultSettings
  override val trigger: PluginTrigger               = noTrigger

  implicit lazy val logger                = Slf4jLogger.getLogger[IO]
  implicit lazy val gh: GithubService[IO] = GithubService.build[IO]

  def compareBenchmarksTask: Def.Initialize[Task[Unit]] = Def.task {

    benchmarkTask(
      previousBenchmarkPath.value,
      currentBenchmarkPath.value,
      keyColumnName.value,
      compareColumnName.value,
      thresholdColumnName.value,
      modeColumnName.value,
      unitsColumnName.value,
      generalThreshold.value,
      benchmarkThreshold.value,
      include.value,
      exclude.value,
      outputToFile.value,
      outputPath.value,
      parseOutputFormat(outputFormat.value)
    ).leftFlatMap(e =>
        EitherT.left[List[BenchmarkComparisonResult]](logger.error(s"There was an error: $e"))
      )
      .void
      .value
      .unsafeRunSync()
      .merge
  }

  def compareBenchmarksCITask: Def.Initialize[Task[Unit]] = Def.task {
    (for {
      basicBenchmark <- benchmarkTask(
        previousBenchmarkPath.value,
        currentBenchmarkPath.value,
        keyColumnName.value,
        compareColumnName.value,
        thresholdColumnName.value,
        modeColumnName.value,
        unitsColumnName.value,
        generalThreshold.value,
        benchmarkThreshold.value,
        include.value,
        exclude.value,
        outputToFile.value,
        outputPath.value,
        parseOutputFormat(outputFormat.value)
      ).leftMap(NonEmptyChain.one)
      params <- EitherT.fromEither[IO](
        GitHubParameters.fromParams(
          gitHubToken.value,
          repositoryOwner.value,
          repositoryName.value,
          pullRequestNumber.value,
          targetUrl.value,
          branch.value,
          commitMessage.value
        )
      )
      _ <- submitResultsToGitHub(
        basicBenchmark,
        previousBenchmarkPath.value,
        currentBenchmarkPath.value,
        params
      ).leftMap(e => NonEmptyChain[HoodError](GitHubConnectionError(e.getMessage)))
    } yield basicBenchmark)
      .leftFlatMap(e =>
        EitherT.left[List[BenchmarkComparisonResult]](
          logger.error(s"Error(s) found: \n${e.toList.mkString("\n")}")
        )
      )
      .void
      .value
      .unsafeRunSync()
      .merge
  }

  def uploadBenchmarkTask: Def.Initialize[Task[Unit]] = Def.task {
    (if (benchmarkFiles.value.isEmpty) {
       logger.error(s"`benchmarkFiles` is empty. Stopping task.")
     } else {
       (for {
         files <- benchmarkFiles.value.traverse(file =>
           EitherT(FileUtils.readFile[IO](file))
             .map(content => (s"${uploadDirectory.value}/${file.getName}", content))
             .leftMap(e => NonEmptyChain.one[HoodError](InputFileError(e.getMessage)))
         )
         params <- EitherT.fromEither[IO](
           GitHubParameters.fromParams(
             gitHubToken.value,
             repositoryOwner.value,
             repositoryName.value,
             pullRequestNumber.value,
             targetUrl.value,
             branch.value,
             commitMessage.value
           )
         )
         _ <- uploadFilesToGitHub[IO](files, params)
           .leftMap(e => NonEmptyChain[HoodError](GitHubConnectionError(e.getMessage)))
       } yield ())
         .leftFlatMap(e =>
           EitherT.left[Unit](
             logger.error(s"Error(s) found: \n${e.toList.mkString("\n")}")
           )
         )
         .value
     }).void.unsafeRunSync()

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
      benchmarkThreshold: Map[String, Double],
      include: Option[String],
      exclude: Option[String],
      shouldOutputToFile: Boolean,
      outputFilePath: File,
      outputFileFormat: OutputFileFormat
  )(
      implicit L: Logger[F],
      S: Sync[F],
      C: Console[F]
  ): EitherT[F, HoodError, List[BenchmarkComparisonResult]] = {

    val columns = BenchmarkColumns(
      keyColumnName,
      modeColumnName,
      compareColumnName,
      thresholdColumnName,
      unitsColumnName
    )

    val csvService: CsvService[F]   = CsvService.build[F]
    val jsonService: JsonService[F] = JsonService.build[F]

    for {
      previousBenchmarks <- EitherT(
        parseBenchmark[F](columns, previousPath, csvService, jsonService)
      ).map(buildBenchmarkMap)
      currentBenchmarks <- EitherT(parseBenchmark[F](columns, currentPath, csvService, jsonService))
        .map(buildBenchmarkMap)
      result <- EitherT.right[HoodError](
        performBenchmarkComparison[F](
          filterBenchmarks(currentBenchmarks, include, exclude),
          filterBenchmarks(previousBenchmarks, include, exclude),
          generalThreshold,
          benchmarkThreshold
        )
      )
      _ <- EitherT(
        writeOutputFile(
          shouldOutputToFile,
          outputFilePath,
          outputFileFormat,
          result,
          previousPath.getName,
          currentPath.getName,
          previousBenchmarks,
          currentBenchmarks
        )
      )
      outputMessage = benchmarkOutput(result, previousPath.getName, currentPath.getName)
      _ <- EitherT.right(C.putStrLn(outputMessage))
    } yield result

  }

  private def submitResultsToGitHub[F[_]](
      benchmarkResult: List[BenchmarkComparisonResult],
      previousPath: File,
      currentPath: File,
      params: GitHubParameters
  )(implicit L: Logger[F], S: Sync[F], G: GithubService[F]): EitherT[F, GHException, Unit] =
    for {
      _ <- EitherT(
        G.publishComment(
          params.accessToken,
          params.repositoryOwner,
          params.repositoryName,
          params.pullRequestNumber,
          s"*sbt-hood* benchmark result:\n\n${benchmarkOutput(benchmarkResult, previousPath.getName, currentPath.getName)}"
        )
      )
      comparison = gitHubStateFromBenchmarks(benchmarkResult)
      _ <- EitherT(
        G.createStatus(
          params.accessToken,
          params.repositoryOwner,
          params.repositoryName,
          params.pullRequestNumber,
          comparison.state,
          params.targetUrl,
          comparison.description,
          GithubModel.githubStatusContext
        )
      )
    } yield ()

  private[this] def uploadFilesToGitHub[F[_]](
      files: List[(String, String)],
      params: GitHubParameters
  )(implicit L: Logger[F], S: Sync[F], G: GithubService[F]): EitherT[F, GHException, Unit] =
    EitherT(
      G.commitFilesAndContents(
        params.accessToken,
        params.repositoryOwner,
        params.repositoryName,
        params.branch,
        params.commitMessage,
        files
      )
    ).void

  def buildBenchmarkMap(benchmarks: List[Benchmark]): Map[String, Benchmark] =
    benchmarks.map(b => (b.benchmark, b)).toMap

  def filterBenchmarks(
      benchmarks: Map[String, Benchmark],
      includeRegExpr: Option[String],
      excludeRegExpr: Option[String]
  ): Map[String, Benchmark] = {
    val includedBenchmarks = includeRegExpr.fold(benchmarks)(inc =>
      benchmarks.filter { case (key, _) => inc.r.pattern.matcher(key).matches }
    )

    val benchmarksAfterExcludes = excludeRegExpr.fold(includedBenchmarks)(exc =>
      includedBenchmarks.filterNot { case (key, _) => exc.r.pattern.matcher(key).matches }
    )

    benchmarksAfterExcludes
  }

  private[this] def performBenchmarkComparison[F[_]](
      currentBenchmarks: Map[String, Benchmark],
      previousBenchmarks: Map[String, Benchmark],
      generalThreshold: Option[Double],
      thresholdMap: Map[String, Double]
  )(implicit L: Logger[F], S: Sync[F]): F[List[BenchmarkComparisonResult]] =
    previousBenchmarks.toList
      .traverse {
        case (benchmarkKey, previous) =>
          val threshold =
            thresholdMap
              .get(benchmarkKey)
              .fold(generalThreshold.getOrElse(previous.primaryMetric.scoreError))(identity)

          currentBenchmarks
            .get(benchmarkKey)
            .fold(
              L.error(
                  s"Benchmark $benchmarkKey existing in previous benchmarks is missing from current ones."
                )
                .as(BenchmarkComparisonResult(previous, None, Warning, threshold))
            )(current => S.delay(BenchmarkService.compare(current, previous, threshold)))
      }

  private[this] def writeOutputFile[F[_]](
      shouldOutputToFile: Boolean,
      outputPath: File,
      outputFileFormat: OutputFileFormat,
      benchmarksResults: List[BenchmarkComparisonResult],
      previousFile: String,
      currentFile: String,
      previousBenchmarks: Map[String, Benchmark],
      currentBenchmarks: Map[String, Benchmark]
  )(implicit S: Sync[F]): F[Either[HoodError, Unit]] =
    if (shouldOutputToFile) {
      val collectedBenchmarks =
        collectBenchmarks(
          previousFile,
          currentFile,
          previousBenchmarks,
          currentBenchmarks,
          benchmarksResults
        )

      val fileContents = outputFileFormat match {
        case OutputFileFormatJson => collectedBenchmarks.asJson.noSpaces
        case OutputFileFormatMd   => benchmarkOutput(benchmarksResults, previousFile, currentFile)
      }

      EitherT(FileUtils.writeFile(outputPath, fileContents))
        .leftMap[HoodError](e => OutputFileError(e.getMessage))
        .value
    } else S.pure(Either.right(()))

  def collectBenchmarks(
      previousFile: String,
      currentFile: String,
      previousBenchmarks: Map[String, Benchmark],
      currentBenchmarks: Map[String, Benchmark],
      benchmarkResults: List[BenchmarkComparisonResult]
  ): List[Benchmark] = {
    def benchmarkResultMark(benchmarkName: String): String =
      benchmarkResults
        .find(_.previous.benchmark.equalsIgnoreCase(benchmarkName))
        .map(_.icon)
        .getOrElse("red_circle".emoji.toString())

    def addFilenameName(map: Map[String, Benchmark], filename: String): List[Benchmark] =
      map.values
        .map(item =>
          item
            .copy(benchmark = s"${benchmarkResultMark(item.benchmark)} ${item.benchmark}.$filename")
        )
        .toList

    def extractFilename(name: String) =
      name.split('.').dropRight(1).mkString(".")

    val groupedCurrent  = addFilenameName(currentBenchmarks, extractFilename(currentFile))
    val groupedPrevious = addFilenameName(previousBenchmarks, extractFilename(previousFile))

    (groupedPrevious ++ groupedCurrent).sorted
  }

  private[this] def benchmarkOutput(
      benchmarks: List[BenchmarkComparisonResult],
      previousFile: String,
      currentFile: String
  ): String = {
    def outputComparisonResult(result: BenchmarkComparisonResult): String =
      s"""
         |# ${result.icon} ${result.previous.benchmark} (Threshold: ${result.threshold})
         |
         ||Benchmark|Value|
         ||---------|-----|
         ||$previousFile|${result.previous.primaryMetric.score.toString}|
         ||$currentFile|${result.current.map(_.primaryMetric.score.toString).getOrElse("N/A")}|
    """.stripMargin

    benchmarks.map(outputComparisonResult).mkString("")
  }

  private[this] def gitHubStateFromBenchmarks(
      benchmarks: List[BenchmarkComparisonResult]
  ): GithubStatusDescription =
    if (benchmarks.exists(_.result == Error)) {
      GithubStatusDescription(GithubStatusError, "Failed benchmark comparison")
    } else {
      GithubStatusDescription(GithubStatusSuccess, "Successful benchmark comparison")
    }

  private[this] def parseBenchmark[F[_]](
      columns: BenchmarkColumns,
      file: File,
      csvService: CsvService[F],
      jsonService: JsonService[F]
  )(implicit S: Sync[F]): F[Either[HoodError, List[Benchmark]]] = {

    FileUtils.fileType(file) match {
      case Csv  => csvService.parseBenchmark(columns, file)
      case Json => jsonService.parseBenchmark(file)
      case _ =>
        S.pure(
          BenchmarkLoadingError(s"Invalid file type for file: ${file.getName}")
            .asLeft[List[Benchmark]]
        )
    }

  }
}
