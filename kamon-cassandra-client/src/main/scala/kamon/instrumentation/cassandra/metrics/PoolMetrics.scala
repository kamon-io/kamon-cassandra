package kamon.instrumentation.cassandra.metrics

import kamon.Kamon
import kamon.instrumentation.cassandra.CassandraInstrumentation
import kamon.instrumentation.cassandra.CassandraInstrumentation.TargetNode
import kamon.metric._
import kamon.tag.TagSet

object PoolMetrics {
  val PoolBorrowTime        = Kamon.timer(
    name = "cassandra.client.pool.borrow-time",
    description = "Time spent acquiring connection from the pool"
  )
  val ConnectionPoolSize    = Kamon.rangeSampler(
    name = "cassandra.client.pool.size",
    description = "Connection pool size for this host"
  )
  val ConnectionPoolGlobalSize    = Kamon.rangeSampler(
    name = "cassandra.client.pool.size",
    description = "Total number of connections across all target nodes"
  )
  val TrashedConnections    = Kamon.counter(
    name = "cassandra.client.pool.trashed",
    description = "Number of trashed connections for this host"
  )
  val InFlightPerConnection = Kamon.histogram(
    name = "cassandra.client.pool.connection.in-flight",
    description = "Number of in-flight request on this connection measured at the moment a new query is issued"
  )
  val InFlightPerTarget     = Kamon.histogram(
    name = "cassandra.client.pool.target.in-flight",
    description = "Number of in-flight request towards this host measured at the moment a new query is issued"
  )

  class PoolInstruments(node: TargetNode) extends InstrumentGroup(CassandraInstrumentation.targetMetricTags(node)) {
    val borrow: Timer                 = register(PoolBorrowTime)
    val size: RangeSampler                = register(ConnectionPoolSize)
    val globalSize: RangeSampler          = ConnectionPoolGlobalSize.withoutTags()
    val trashedConnections: Counter       = register(TrashedConnections)
    val inflightPerConnection: Histogram  = register(InFlightPerConnection)
    val inflightPerHost: Histogram        = register(InFlightPerTarget)
  }
}


