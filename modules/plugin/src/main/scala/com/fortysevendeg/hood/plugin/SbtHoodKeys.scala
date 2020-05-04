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

import sbt._
import Keys._
import com.fortysevendeg.hood.plugin.SbtHoodPlugin._

trait SbtHoodKeys {

  val compareBenchmarks: TaskKey[Unit] =
    taskKey[Unit]("Compare two benchmarks and show results through the console.")
  val compareBenchmarksCI: TaskKey[Unit] = taskKey[Unit](
    "Compare two benchmarks and show results through a GitHub comment in a repository."
  )
  val uploadBenchmarks: TaskKey[Unit] =
    taskKey[Unit]("Uploads the provided list of benchmark output files as a GitHub commit.")

  val previousBenchmarkPath: SettingKey[File] = settingKey(
    "Path to the previous JMH benchmark in CSV format. By default: {project_root}/master.csv."
  )
  val currentBenchmarkPath: SettingKey[File] = settingKey(
    "Path to the current JMH benchmark in CSV format.  By default: {project_root}/current.csv."
  )
  val keyColumnName: SettingKey[String] = settingKey(
    "Column name to distinguish each benchmark on the comparison. By default: `Benchmark`."
  )
  val compareColumnName: SettingKey[String] = settingKey(
    "Column name of the column to compare (values must be `Double`). By default: Score."
  )
  val thresholdColumnName: SettingKey[String] = settingKey(
    "Column name to get the threshold per benchmark. By default: `Score Error (99.9%)`."
  )
  val modeColumnName: SettingKey[String] = settingKey(
    "Column name to get the benchmark mode. By default: `Mode`."
  )
  val unitsColumnName: SettingKey[String] = settingKey(
    "Column name to get the benchmark mode. By default: `Unit`."
  )
  val generalThreshold: SettingKey[Option[Double]] = settingKey(
    "Common threshold to all benchmarks overriding the value coming from `thresholdColumnName`. Optional."
  )
  val benchmarkThreshold: SettingKey[Map[String, Double]] = settingKey(
    "Map with a custom threshold per benchmark key overriding the value coming from `thresholdColumnName` or `generalThreshold`. Optional."
  )
  val include: SettingKey[Option[String]] = settingKey(
    "Regular expression to include only the benchmarks with a matching key. Optional"
  )
  val exclude: SettingKey[Option[String]] = settingKey(
    "Regular expression to exclude the benchmarks with a matching key. Optional"
  )
  val outputToFile: SettingKey[Boolean] = settingKey(
    "True if sbt-hood should write the benchmark output to a file. By default: `false`."
  )
  val outputPath: SettingKey[File] = settingKey(
    "Path to the output file. By default: `{project_root}/comparison.md`"
  )
  val outputFormat: SettingKey[String] = settingKey(
    "Output file format. `MD` and `JSON` are supported. By default: `MD`"
  )
  val token: SettingKey[Option[String]] = settingKey(
    "GitHub access token required by `compareBenchmarksCI`."
  )
  val repositoryOwner: SettingKey[Option[String]] = settingKey(
    "Owner of the repository where the plugin will post updates, required by `compareBenchmarksCI`."
  )
  val repositoryName: SettingKey[Option[String]] = settingKey(
    "Name of the repository where the plugin will post updates, required by `compareBenchmarksCI`."
  )
  val pullRequestNumber: SettingKey[Option[Int]] = settingKey(
    "Pull request number where the plugin will post updates, required by `compareBenchmarksCI`."
  )
  val targetUrl: SettingKey[Option[String]] = settingKey(
    "URL to the CI job, used by `compareBenchmarksCI`."
  )
  val shouldBlockMerge: SettingKey[Boolean] = settingKey(
    "If set to `true`, blocks the mergeability of the current PR if any of the benchmarks fail. By default: `true`."
  )
  val benchmarkFiles: SettingKey[List[File]] = settingKey(
    "Files to be uploaded, used by `uploadBenchmarks`. Default: empty list."
  )
  val uploadDirectory: SettingKey[String] = settingKey(
    "Target path in the repository to upload benchmark files, used by `uploadBenchmarks`. By default: `benchmarks`."
  )
  val commitMessage: SettingKey[String] = settingKey(
    "Commit message to include when uploading benchmark files, used by `uploadBenchmarks`. By default: `Upload benchmark`."
  )
  val branch: SettingKey[String] = settingKey(
    "Target branch for benchmark files uploads, used by `uploadBenchmarks`. By default: `master`."
  )
}

object SbtHoodKeys extends SbtHoodKeys

trait SbtHoodDefaultSettings extends SbtHoodKeys {

  def defaultSettings =
    Seq(
      previousBenchmarkPath := baseDirectory.value / "master.csv",
      currentBenchmarkPath := baseDirectory.value / "current.csv",
      keyColumnName := "Benchmark",
      compareColumnName := "Score",
      thresholdColumnName := "Score Error (99.9%)",
      modeColumnName := "Mode",
      unitsColumnName := "Unit",
      generalThreshold := None,
      benchmarkThreshold := Map.empty,
      exclude := None,
      include := None,
      outputToFile := false,
      outputPath := baseDirectory.value / "comparison.md",
      outputFormat := "MD",
      token := None,
      repositoryOwner := None,
      repositoryName := None,
      pullRequestNumber := None,
      targetUrl := None,
      shouldBlockMerge := true,
      benchmarkFiles := List.empty,
      uploadDirectory := "benchmarks",
      commitMessage := "Upload benchmark",
      branch := "master",
      compareBenchmarks := compareBenchmarksTask.value,
      compareBenchmarksCI := compareBenchmarksCITask.value,
      uploadBenchmarks := uploadBenchmarksTask.value
    )

}
