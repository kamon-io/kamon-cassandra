package kamon.instrumentation.cassandra.metrics

trait HasPoolMetrics {
  def setMetrics(metrics: PoolMetrics): Unit
  def getMetrics: PoolMetrics
}

class PoolWithMetrics extends HasPoolMetrics {
  private var _metrics: PoolMetrics = _
  def setMetrics(metrics: PoolMetrics): Unit = _metrics = metrics
  def getMetrics: PoolMetrics = _metrics
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