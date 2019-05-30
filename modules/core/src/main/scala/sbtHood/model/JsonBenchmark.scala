package sbtHood.model

final case class JsonBenchmark(
    benchmark: String,
    mode: String,
    primaryMetric: JsonPrimaryMetric,
    secondaryMetric: JsonSecondMetric
)

final case class JsonPrimaryMetric(
    score: Double,
    scoreError: Double,
    scoreUnit: String,
    rawData: List[List[Double]]
)

final case class JsonSecondMetric()
