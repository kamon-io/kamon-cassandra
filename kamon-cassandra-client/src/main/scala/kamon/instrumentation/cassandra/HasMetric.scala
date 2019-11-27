package kamon.instrumentation.cassandra

import com.datastax.driver.core.Host
import kamon.instrumentation.cassandra.client.{CassandraClientMetrics, TargetResolver}
import kamon.metric.{Counter, Histogram, RangeSampler}

trait HasPoolMetrics {
  def setMetrics(metrics: HostPoolMetrics): Unit
  def getMetrics: HostPoolMetrics
}

class PoolWithMetrics extends HasPoolMetrics {
  private var _metrics: HostPoolMetrics = _
  def setMetrics(metrics: HostPoolMetrics): Unit = _metrics = metrics
  def getMetrics: HostPoolMetrics = _metrics
}

//TODO pass in tagSet or?1
class HostPoolMetrics(host: Host) {
  private val target: String = TargetResolver.getTarget(host.getAddress)
  //TODO depending on config, might not want to tag target

  val borrow: Histogram = CassandraClientMetrics.poolBorrow(target)
  val size: RangeSampler = CassandraClientMetrics.connections(target)
  val trashedConnections = CassandraClientMetrics.trashedConnections(target)
  val inflightPerConnection: Histogram = CassandraClientMetrics.inflightPerConnection(target) //Number of requests inflight on this connect
  val inflightPerHost: Histogram = CassandraClientMetrics.inflightPerTarget(target)

}
