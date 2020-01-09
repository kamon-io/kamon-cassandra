package kamon.instrumentation.cassandra.metrics

import java.util.concurrent.ScheduledFuture


trait HasPoolMetrics {
  def setMetrics(proxy: MetricProxy): Unit
  def getMetrics: MetricProxy
  def setSampling(future: ScheduledFuture[_])
  def getSampling: ScheduledFuture[_]
}

class PoolWithMetrics extends HasPoolMetrics {
  private var _metricProxy: MetricProxy = _
  private var _sampling: ScheduledFuture[_] = _

  def setMetrics(proxy: MetricProxy): Unit = _metricProxy = proxy
  def getMetrics: MetricProxy = _metricProxy

  def setSampling(future: ScheduledFuture[_]): Unit = _sampling = future
  def getSampling: ScheduledFuture[_] = _sampling
}

