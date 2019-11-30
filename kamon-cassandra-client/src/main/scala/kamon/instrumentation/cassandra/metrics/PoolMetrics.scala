package kamon.instrumentation.cassandra.metrics

import kamon.Kamon
import kamon.instrumentation.cassandra.Cassandra
import kamon.instrumentation.cassandra.Cassandra.TargetNode
import kamon.metric._

//TODO descriptions
object PoolMetrics {
  val PoolBorrowTime        = Kamon.histogram(
    "cassandra.client.pool.borrow-time",
    "Time spent acquiring connection from the pool",
    MeasurementUnit.time.nanoseconds
  )
  val ConnectionPoolSize    = Kamon.rangeSampler(
    "cassandra.client.pool.size",
    "Connection pool size for this host"
  )
  val TrashedConnections    = Kamon.counter(
    "cassandra.client.pool.trashed",
    "Number of trashed connections for this host"
  )
  val InFlightPerConnection = Kamon.histogram(
    "cassandra.client.pool.inflight-per-connection",
    "Number of in-flight request on this connection measured at the moment a new query is issued"
  )
  val InFlightPerTarget     = Kamon.histogram(
    "cassandra.client.inflight-per-target",
    "Number of in-flight request towards this host measured at the moment a new query is issued"
  )
}

class PoolMetrics(node: TargetNode) {
  import PoolMetrics._

  val targetTags = Cassandra.targetTags(node)

  val borrow: Histogram                 = PoolBorrowTime.withTags(targetTags)
  val size: RangeSampler                = ConnectionPoolSize.withTags(targetTags)
  val trashedConnections: Counter       = TrashedConnections.withTags(targetTags)
  val inflightPerConnection: Histogram  = InFlightPerConnection.withTags(targetTags)
  val inflightPerHost: Histogram        = InFlightPerTarget.withTags(targetTags)
}