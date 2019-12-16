package kamon.instrumentation.cassandra.metrics

import kamon.instrumentation.cassandra.metrics.PoolMetrics.PoolInstruments
import kamon.instrumentation.cassandra.metrics.QueryMetrics.QueryInstruments

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
  def setMetrics(metrics: QueryInstruments): Unit
  def getMetrics: QueryInstruments
}

class PoolWithQueryMetrics extends HasQueryMetrics {
  private var _metrics: QueryInstruments = _
  override def setMetrics(metrics: QueryInstruments): Unit = _metrics = metrics
  override def getMetrics: QueryInstruments = _metrics
}