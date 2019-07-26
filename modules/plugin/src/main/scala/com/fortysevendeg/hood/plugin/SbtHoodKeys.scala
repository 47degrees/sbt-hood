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

  import SbtHoodKeys._

  def defaultSettings = Seq(
    previousBenchmarkPath := baseDirectory.value / "master.csv",
    currentBenchmarkPath := baseDirectory.value / "current.csv",
    compareBenchmarks := (Compile / packageBin / artifactPath).value,
    keyColumnName := "Benchmark",
    compareColumnName := "Score",
    thresholdColumnName := "Score Error (99.9%)",
    generalThreshold := None,
    benchmarkThreshold := Map.empty
  )

}