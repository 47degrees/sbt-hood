---
layout: docs
title: Getting Started
permalink: /
---

# Getting started
## Benchmark comparisons

*sbt-hood* is able to perform comparisons based on JMH benchmarks. Providing CSV benchmarks files for
both the current state of your code and any new changes you want to introduce, *sbt-hood* will make
sure your project performs within your expected thresholds.

It can work in a standalone basis or, more interestingly, within your CI pipelines. This allows
automated comparisons to be performed and, should your performance variables get lower than expected,
block a GitHub pull request to be merged.

## Standalone use

In order to use *sbt-hood* you simply have to specify what benchmark files do you want to compare,
(and the name of the important columns if your benchmark is in CSV format) through the following
sbt variables:

* `previousBenchmarkPath`: path to the previous JMH benchmark in CSV/Json format. By default: `{project_root}/master.csv`.
* `currentBenchmarkPath`: path to the current JMH benchmark in CSV/Json format.  By default: ``{project_root}/current.csv`.
* `keyColumnName`: column name to distinguish each benchmark on the comparison. By default: `Benchmark`.
* `compareColumnName`: column name of the column to compare (values must be `Double`). By default: `Score`.
* `thresholdColumnName`: column name to get the threshold per benchmark. By default: `Score Error (99.9%)`.
* `modeColumnName`: column name to get the benchmark mode. By default: `Mode`.
* `unitsColumnName`: column name to get the benchmark mode. By default: `Unit`.
* `generalThreshold`: optional common threshold to all benchmarks overriding the value coming from `thresholdColumnName`.
* `benchmarkThreshold`: optional map with a custom threshold per benchmark key overriding the value coming from `thresholdColumnName` or `generalThreshold`.
* `outputToFile`: set to `true` saves the comparison results in a separate file in addition to the console report. By default this setting is disabled.
* `outputPath`: path to the comparison report to be generated. By default: `{project_root}/comparison.md`.
* `outputFormat`: file format for the comparison report. `MD` and `JSON` are supported. By default: `MD`.

As you can see all these variables have default values or are optional so you'll just need to adapt
some of these if your benchmark files are in CSV format and contain different column names. Once set
up, you only need to run a benchmark comparison through the following command:

```
compareBenchmarks
```

If no errors are found, you'll see the comparisons being output in the sbt console. i.e.:

```
# ✔ test.decoding (Threshold: 3.0)

|Benchmark|Value|
|---------|-----|
|previous.json|5.0|
|current_better.json|6.0|

# ✔ test.parsing (Threshold: 3.0)

|Benchmark|Value|
|---------|-----|
|previous.json|6.0|
|current_better.json|7.0|
```

For each benchmark (set by the value found under the specified `keyColumnName` variable) you'll see
a table comparing the values of both files. Results can be of three types (visible by the icon that
shows in each table):

* Success (✔): the current benchmark is better than the previous one.
* Warning (⚠): the current benchmark is worse than the previous one, but within the specified threshold.
* Error (🔴): the current benchmark is worse than the previous one, and outside the specified threshold.
