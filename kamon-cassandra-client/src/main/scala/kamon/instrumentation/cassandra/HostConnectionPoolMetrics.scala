package kamon.instrumentation.cassandra

import kamon.Kamon
import kamon.instrumentation.cassandra.CassandraInstrumentation.Node
import kamon.metric._

object HostConnectionPoolMetrics {
  private val poolPrefix = "cassandra.client.session.host"


  val BorrowTime        = Kamon.timer(
    name = poolPrefix + "borrow-time",
    description = "Time spent acquiring connection from the pool"
  )
  val Size    = Kamon.rangeSampler(
    name = poolPrefix + "size",
    description = "Connection pool size for this host"
  )

  val InFlight = Kamon.histogram(
    name = poolPrefix + "in-flight",
    description = "Number of in-flight request on this connection measured at the moment a new query is issued"
  )

  val Errors = Kamon.counter(
    name = poolPrefix + "errors",
    description = "Number of client errors during execution" //TODO should this include server errors
  )

  val Timeouts = Kamon.counter(
    name = poolPrefix + "timeouts",
    description = "Number of timed-out executions"
  )

  val Canceled = Kamon.counter(
    name = poolPrefix + "canceled",
    description = "Number of canceled executions"
  )

  val TriggeredSpeculations = Kamon.counter(
    name = poolPrefix + "retries",
    description = "Number of retried executions"
  )

  //TODO includes all target tags (dc, rack, cluster, ip) irregardles config, can only be enabled/disabled completely
  class HostConnectionPoolInstruments(node: Node) extends InstrumentGroup(CassandraInstrumentation.nodeMetricTags(node)) {

    val borrow: Timer                     = register(BorrowTime)
    val size: RangeSampler                = register(Size)
    val inFlight: Histogram               = register(InFlight)
    val errors: Counter                   = register(Errors)
    val timeouts: Counter                 = register(Timeouts)
    val canceled: Counter                 = register(Canceled)
    val triggeredSpeculations: Counter    = register(TriggeredSpeculations)
  }
}