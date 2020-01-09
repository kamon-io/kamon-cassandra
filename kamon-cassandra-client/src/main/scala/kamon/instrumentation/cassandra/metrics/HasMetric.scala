package kamon.instrumentation.cassandra.metrics

import kamon.instrumentation.cassandra.metrics.HostConnectionPoolMetrics.HostConnectionPoolInstruments
import kamon.instrumentation.cassandra.metrics.QueryMetrics.QueryInstruments

trait HasPoolMetrics {
  def setMetrics(metrics: HostConnectionPoolInstruments): Unit
  def getMetrics: HostConnectionPoolInstruments
}

class PoolWithMetrics extends HasPoolMetrics {
  private var _metrics: HostConnectionPoolInstruments = _
  def setMetrics(metrics: HostConnectionPoolInstruments): Unit = _metrics = metrics
  def getMetrics: HostConnectionPoolInstruments = _metrics
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