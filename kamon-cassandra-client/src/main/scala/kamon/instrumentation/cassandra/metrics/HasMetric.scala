package kamon.instrumentation.cassandra.metrics


trait HasPoolMetrics {
  def setMetrics(proxy: MetricProxy): Unit
  def getMetrics: MetricProxy
}

class PoolWithMetrics extends HasPoolMetrics {
  private var _metricProxy: MetricProxy = _

  def setMetrics(proxy: MetricProxy): Unit = _metricProxy = proxy
  def getMetrics: MetricProxy = _metricProxy
}

