package kamon.instrumentation.cassandra.metrics

import kamon.Kamon
import kamon.instrumentation.cassandra.CassandraInstrumentation
import kamon.instrumentation.cassandra.CassandraInstrumentation.TargetNode
import kamon.metric.{Counter, InstrumentGroup, RangeSampler, Timer}


object SessionMetrics {
  private val sessionPrefix = "cassandra.client.session"

  val PoolBorrowTime        = Kamon.timer(
    name = sessionPrefix + "borrow-time",
    description = "Time spent acquiring connection from the pool"
  )
  val ConnectionPoolSize    = Kamon.rangeSampler(
    name = sessionPrefix + "size",
    description = "Connection pool size for this host"
  )
  val TrashedConnections    = Kamon.counter(
    name = sessionPrefix + "trashed",
    description = "Number of trashed connections for this host"
  )
  val InFlight = Kamon.rangeSampler(
    name = sessionPrefix + "in-flight",
    description = "Number of in-flight requests in this session"
  )

  val Speculations = Kamon.counter(
    name = sessionPrefix + "speculative-executions",
    description = "Number of speculative executions performed"
  )

  val Retries = Kamon.counter(
    name = sessionPrefix + "retries",
    description = "Number of retried executions"
  )

  val Errors = Kamon.counter(
    name = sessionPrefix + "errors",
    description = "Number of client errors during ececution" //TODO should this include server errors
  )

  val Timeouts = Kamon.counter(
    name = sessionPrefix + "timeouts",
    description = "Number of timed-out executions"
  )

  val Canceled = Kamon.counter(
    name = sessionPrefix + "canceled",
    description = "Number of canceled executions"
  )

  class SessionInstruments(node: TargetNode) extends InstrumentGroup(CassandraInstrumentation.targetMetricTags(node)) { //TODO only cluster tags
    val trashedConnections: Counter       = register(TrashedConnections)
    val borrow: Timer                     = register(PoolBorrowTime)
    val size: RangeSampler                = register(ConnectionPoolSize)
    val inFlightRequests: RangeSampler    = register(InFlight)
    val speculations: Counter             = register(Speculations)
    val retries: Counter                  = register(Retries)
    val errors: Counter                   = register(Errors)
    val timeouts: Counter                 = register(Timeouts)
    val canceled: Counter                 = register(Canceled)
  }
}