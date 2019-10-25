---
layout: home
technologies:
 - first: ["Scala", "sbt-hood is an sbt plugin to compare benchmarks, allowing you to check the performance of your application both in your console and your GitHub pull requests."]
 - second: ["CI", "sbt-hood is ideal to integrate within your current CI pipelines to keep the performance of your application in check."]
 - third: ["GitHub", "sbt-hood integrates with GitHub's API so you can always be sure all code iterations won't decrease the performance of your applications."]
---

# sbt-hood

*sbt-hood* is an sbt plugin to compare JMH benchmarks and set the result as a GitHub status for a pull request.

## Installation

To get started with *sbt-hood* simply add the following to your `plugins.sbt` file:

```
addSbtPlugin("com.47deg" %% "sbt-hood-plugin" % "0.0.1")
```
