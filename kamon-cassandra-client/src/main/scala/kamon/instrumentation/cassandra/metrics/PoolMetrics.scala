package kamon.instrumentation.cassandra.metrics

import kamon.Kamon
import kamon.instrumentation.cassandra.Cassandra
import kamon.instrumentation.cassandra.Cassandra.TargetNode
import kamon.metric._
import kamon.tag.TagSet

object PoolMetrics {
  val PoolBorrowTime        = Kamon.histogram(
    name = "cassandra.client.pool.borrow-time",
    description = "Time spent acquiring connection from the pool",
    unit = MeasurementUnit.time.nanoseconds
  )
  val ConnectionPoolSize    = Kamon.rangeSampler(
    name = "cassandra.client.pool.size",
    description = "Connection pool size for this host"
  )
  val TrashedConnections    = Kamon.counter(
    name = "cassandra.client.pool.trashed",
    description = "Number of trashed connections for this host"
  )
  val InFlightPerConnection = Kamon.histogram(
    name = "cassandra.client.pool.inflight-per-connection",
    description = "Number of in-flight request on this connection measured at the moment a new query is issued"
  )
  val InFlightPerTarget     = Kamon.histogram(
    name = "cassandra.client.inflight-per-target",
    description = "Number of in-flight request towards this host measured at the moment a new query is issued"
  )

  class PoolInstruments(node: TargetNode) extends InstrumentGroup(Cassandra.targetTags(node)) {
    val borrow: Histogram                 = register(PoolBorrowTime)
    val size: RangeSampler                = register(ConnectionPoolSize)
    val trashedConnections: Counter       = register(TrashedConnections)
    val inflightPerConnection: Histogram  = register(InFlightPerConnection)
    val inflightPerHost: Histogram        = register(InFlightPerTarget)
  }
}


