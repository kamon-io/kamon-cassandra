package kamon.instrumentation.cassandra.metrics

import com.datastax.driver.core.Host
import kamon.Kamon
import kamon.instrumentation.cassandra.CassandraInstrumentation
import kamon.instrumentation.cassandra.CassandraInstrumentation.TargetNode
import kamon.metric.{Counter, InstrumentGroup}
import kamon.trace.Span

object QueryMetrics {
  val Errors = Kamon.counter(
    name = "cassandra.query.errors",
    description = "Count of executions that resulted in error"
  )
  val Timeouts = Kamon.counter(
    name = "cassandra.query.timeouts",
    description = "Count of executions that timed-out"
  )
  val RetriedExecutions = Kamon.counter(
    name = "cassandra.query.retries",
    description = "Count of executions that were retries" //TODO actually, successeful executions, not al
  )
  val SpeculativeExecutions = Kamon.counter(
    name = "cassandra.query.speculative",
    description = "Count of executions that were triggered by speculative execution strategy"
  )
  val CanceledExecutions = Kamon.counter(
    name = "cassandra.query.cancelled",
    description = "Count of executions that were cancelled mid-flight"
  )

  def instrumentsForHost(host: Host, clusterName: String): QueryInstruments = new QueryInstruments(CassandraInstrumentation.targetFromHost(host, clusterName))

  class QueryInstruments(targetNode: TargetNode) extends InstrumentGroup(CassandraInstrumentation.targetMetricTags(targetNode)) {

    def tagSpan(span: Span): Unit = {
      CassandraInstrumentation.tagSpanWithTarget(targetNode, span)
    }

    val errors: Counter = register(Errors)
    val timeouts: Counter = register(Timeouts)

    /*Here it would be more valuable to tag with host that's being retried or speculated on than
    * one defined by a policy so we are dropping it altogether */
    val retries: Counter = register(RetriedExecutions)
    val speculative: Counter = register(SpeculativeExecutions)
    val cancelled: Counter = register(CanceledExecutions)
  }

}


