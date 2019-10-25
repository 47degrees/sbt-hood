---
layout: docs
title: Getting Started
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
and the name of the important columns in the CSV file through the following sbt variables:

* `previousBenchmarkPath`: path to the previous JMH benchmark in CSV format. By default: `{project_root}/master.csv`.
* `currentBenchmarkPath`: path to the current JMH benchmark in CSV format.  By default: ``{project_root}/current.csv`.
* `keyColumnName`: column name to distinguish each benchmark on the comparison. By default: `Benchmark`.
* `compareColumnName`: column name of the column to compare (values must be `Double`). By default: `Score`.
* `thresholdColumnName`: column name to get the threshold per benchmark. By default: `Score Error (99.9%)`.
* `modeColumnName`: column name to get the benchmark mode. By default: `Mode`.
* `unitsColumnName`: column name to get the benchmark mode. By default: `Unit`.
* `generalThreshold`: optional common threshold to all benchmarks overriding the value coming from `thresholdColumnName`.
* `benchmarkThreshold`: optional map with a custom threshold per benchmark key overriding the value coming from `thresholdColumnName` or `generalThreshold`.

As you can see all these variables have default values or are optional so you'll just need to adapt
some of these if your CSV files contain different column names and the like. Once set up, you only
need to run a benchmark comparison through the following command:

```
compareBenchmarks
```

If no errors are found, you'll see the comparisons being output in the sbt console. i.e.:

```
✔ test.decoding (Threshold: 4.0)

Benchmark|Value
master.csv|5.0
current.csv|7.0

✔ test.parsing (Threshold: 1.0)

Benchmark|Value
master.csv|6.0
current.csv|10.0
```

For each benchmark (set by the value found under the specified `keyColumnName` variable) you'll see
a table comparing the values of both files. Results can be of three types (visible by the icon that
shows in each table):

* Success: the current benchmark is better than the previous one.
* Warning: the current benchmark is worse than the previous one, but within the specified threshold.
* Error: the current benchmark is worse than the previous one, and outside the specified threshold.