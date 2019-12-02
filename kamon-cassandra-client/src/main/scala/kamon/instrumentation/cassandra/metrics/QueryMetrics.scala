package kamon.instrumentation.cassandra.metrics

import com.datastax.driver.core.Host
import kamon.Kamon
import kamon.instrumentation.cassandra.Cassandra
import kamon.instrumentation.cassandra.Cassandra.TargetNode
import kamon.metric.{Counter, InstrumentGroup}
import kamon.trace.Span

object QueryMetrics {
  val Errors                = Kamon.counter(name = "cassandra.query.errors", description = "Count of executions that resulted in error")
  val Timeouts              = Kamon.counter(name = "cassandra.query.timeouts", description =  "Count of executions that timed-out")
  val RetriedExecutions     = Kamon.counter(name = "cassandra.query.retries", description = "Count of executions that were retries")
  val SpeculativeExecutions = Kamon.counter(name = "cassandra.query.speculative", description = "Count of executions that were triggered by speculative execution strategy")
  val CanceledExecutions    = Kamon.counter(name = "cassandra.query.cancelled", description = "Count of executions that were cancelled mid-flight")

  def forHost(host: Host): QueryMetrics = new QueryMetrics(Cassandra.targetFromHost(host))
}
                                                                    //TODO filter these out to be only metricTags
class QueryMetrics(targetNode: TargetNode) extends InstrumentGroup(Cassandra.targetMetricTags(targetNode)) {
  import QueryMetrics._

  def tagSpan(span: Span): Unit = {
    Cassandra.tagSpanWithTarget(targetNode, span)
  }

  val errors: Counter   = register(Errors)
  val timeouts: Counter = register(Timeouts)

  /*Here it would be more valuable to tag with host that's being retried or speculated on than
  * one defined by a policy so we are dropping it altogether */
  val retries: Counter                = RetriedExecutions.withoutTags()
  val speculative: Counter            = SpeculativeExecutions.withoutTags()
  val cancelled: Counter              = CanceledExecutions.withoutTags()
}


