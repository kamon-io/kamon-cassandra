package kamon.instrumentation.cassandra.metrics

import java.util.concurrent.ScheduledFuture


trait HasPoolMetrics {
  def setMetrics(proxy: NodeMonitor): Unit
  def getMetrics: NodeMonitor
  def setSampling(future: ScheduledFuture[_])
  def getSampling: ScheduledFuture[_]
}

class PoolWithMetrics extends HasPoolMetrics {
  private var _metricProxy: NodeMonitor = _
  private var _sampling: ScheduledFuture[_] = _

  def setMetrics(proxy: NodeMonitor): Unit = _metricProxy = proxy
  def getMetrics: NodeMonitor = _metricProxy

  def setSampling(future: ScheduledFuture[_]): Unit = _sampling = future
  def getSampling: ScheduledFuture[_] = _sampling
}

