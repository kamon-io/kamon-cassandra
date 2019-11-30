package kamon.instrumentation.cassandra.metrics

import com.datastax.driver.core.Host
import kamon.Kamon
import kamon.instrumentation.cassandra.Cassandra
import kamon.instrumentation.cassandra.Cassandra.TargetNode
import kamon.metric.Counter
import kamon.trace.Span
//TODO descriptions
object QueryMetrics {
  val Errors                = Kamon.counter("cassandra.query.errors", "Count of executions that resulted in error")
  val Timeouts              = Kamon.counter("cassandra.query.timeouts", "Count of executions that timed-out")
  val RetriedExecutions     = Kamon.counter("cassandra.query.retries", "Count of executions that were retries")
  val SpeculativeExecutions = Kamon.counter("cassandra.query.speculative", "Count of executions that were triggered by speculative execution strategy")
  val CanceledExecutions    = Kamon.counter("cassandra.query.cancelled", "Count of executions that were cancelled mid-flight")

  def forHost(host: Host): QueryMetrics = new QueryMetrics(Cassandra.targetFromHost(host))
}

class QueryMetrics(targetNode: TargetNode) {
  import QueryMetrics._
  private val targetTags = Cassandra.targetTags(targetNode)

  def tagSpanMetrics(span: Span): Span = span.tagMetrics(targetTags)
  def tagSpan(span: Span): Span = span.tag(targetTags)

  val errors: Counter   = Errors.withTags(targetTags)
  val timeouts: Counter = Timeouts.withTags(targetTags)

  /*Here it would be more valuable to tag with host that's being retried or speculated on than
  * one defined by a policy so we are dropping it altogether */
  val retries: Counter                = RetriedExecutions.withoutTags()
  val speculative: Counter            = SpeculativeExecutions.withoutTags()
  val cancelled: Counter              = CanceledExecutions.withoutTags()
}


