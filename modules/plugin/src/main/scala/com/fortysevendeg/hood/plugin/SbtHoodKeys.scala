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

import sbt._
import Keys._

trait SbtHoodKeys {

  val compareBenchmarks: TaskKey[Unit] =
    taskKey[Unit]("Compare two benchmarks and show results through the console.")
  val compareBenchmarksCI: TaskKey[Unit] = taskKey[Unit](
    "Compare two benchmarks and show results through a GitHub comment in a repository.")

  val previousBenchmarkPath: SettingKey[File] = settingKey(
    "Path to the previous JMH benchmark in CSV format. By default: {project_root}/master.csv.")
  val currentBenchmarkPath: SettingKey[File] = settingKey(
    "Path to the current JMH benchmark in CSV format.  By default: {project_root}/current.csv.")
  val keyColumnName: SettingKey[String] = settingKey(
    "Column name to distinguish each benchmark on the comparison. By default: `Benchmark`.")
  val compareColumnName: SettingKey[String] = settingKey(
    "Column name of the column to compare (values must be `Double`). By default: Score.")
  val thresholdColumnName: SettingKey[String] = settingKey(
    "Column name to get the threshold per benchmark. By default: `Score Error (99.9%)`.")
  val modeColumnName: SettingKey[String] = settingKey(
    "Column name to get the benchmark mode. By default: `Mode`.")
  val unitsColumnName: SettingKey[String] = settingKey(
    "Column name to get the benchmark mode. By default: `Unit`.")
  val generalThreshold: SettingKey[Option[Double]] = settingKey(
    "Common threshold to all benchmarks overriding the value coming from `thresholdColumnName`. Optional.")
  val benchmarkThreshold: SettingKey[Map[String, Double]] = settingKey(
    "Map with a custom threshold per benchmark key overriding the value coming from `thresholdColumnName` or `generalThreshold`. Optional.")

  val gitHubToken: SettingKey[String] = settingKey(
    "GitHub access token required by `compareBenchmarksCI`.")
  val gitHubUserId: SettingKey[String] = settingKey(
    "GitHub ID for the user publishing the benchmark comments, required by `compareBenchmarksCI`.")
  val repositoryOwner: SettingKey[String] = settingKey(
    "Owner of the repository where the plugin will post updates, required by `compareBenchmarksCI`.")
  val repositoryName: SettingKey[String] = settingKey(
    "Name of the repository where the plugin will post updates, required by `compareBenchmarksCI`.")
  val pullRequestNumber: SettingKey[Int] = settingKey(
    "Pull request number where the plugin will post updates, required by `compareBenchmarksCI`.")

}

object SbtHoodKeys extends SbtHoodKeys

trait SbtHoodDefaultSettings extends SbtHoodKeys {

  def defaultSettings = Seq(
    previousBenchmarkPath := baseDirectory.value / "master.csv",
    currentBenchmarkPath := baseDirectory.value / "current.csv",
    keyColumnName := "Benchmark",
    compareColumnName := "Score",
    thresholdColumnName := "Score Error (99.9%)",
    modeColumnName := "Mode",
    unitsColumnName := "Unit",
    generalThreshold := None,
    benchmarkThreshold := Map.empty
  )

}