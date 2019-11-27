package kamon.instrumentation.cassandra

import com.datastax.driver.core.Host
import kamon.instrumentation.cassandra.client.{CassandraClientMetrics, TargetResolver}

trait HasPoolMetrics {
  def set(metrics: HostPoolMetrics): Unit
  def get: HostPoolMetrics
}

class PoolWithMetrics extends HasPoolMetrics {
  private var _metrics: HostPoolMetrics = _
  def set(metrics: HostPoolMetrics): Unit = _metrics = metrics
  def get: HostPoolMetrics = _metrics
}

class HostPoolMetrics(host: Host) {
  private val target = TargetResolver.getTarget(host.getAddress)
  val borrow = CassandraClientMetrics.poolBorrow(target)
  val inflight = CassandraClientMetrics.inflightPerConnection
}
