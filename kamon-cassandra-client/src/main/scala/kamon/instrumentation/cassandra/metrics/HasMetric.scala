package kamon.instrumentation.cassandra.metrics

import kamon.instrumentation.cassandra.metrics.PoolMetrics.PoolInstruments

trait HasPoolMetrics {
  def setMetrics(metrics: PoolInstruments): Unit
  def getMetrics: PoolInstruments
}

class PoolWithMetrics extends HasPoolMetrics {
  private var _metrics: PoolInstruments = _
  def setMetrics(metrics: PoolInstruments): Unit = _metrics = metrics
  def getMetrics: PoolInstruments = _metrics
}



trait HasQueryMetrics {
  def setMetrics(metrics: QueryMetrics): Unit
  def getMetrics: QueryMetrics
}

class PoolWithQueryMetrics extends HasQueryMetrics {
  private var _metrics: QueryMetrics = _
  override def setMetrics(metrics: QueryMetrics): Unit = _metrics = metrics
  override def getMetrics: QueryMetrics = _metrics
}