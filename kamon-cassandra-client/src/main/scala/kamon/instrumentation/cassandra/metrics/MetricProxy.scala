package kamon.instrumentation.cassandra.metrics

import kamon.instrumentation.cassandra.CassandraInstrumentation
import kamon.instrumentation.cassandra.CassandraInstrumentation.TargetNode
import kamon.instrumentation.cassandra.metrics.HostConnectionPoolMetrics.HostConnectionPoolInstruments
import kamon.instrumentation.cassandra.metrics.SessionMetrics.SessionInstruments
import kamon.metric.Timer
import kamon.tag.TagSet
import kamon.trace.Span


class MetricProxy(node: TargetNode) {
  val sessionMetrics = new SessionInstruments(node)
  val poolMetrics = new HostConnectionPoolInstruments(node)

  def tagSpan(span: Span): Unit = {
    CassandraInstrumentation.tagSpanWithTarget(node, span)
  }

  def poolMetricsEnabled = CassandraInstrumentation.settings.poolMetrics


  def connectionsOpened(count: Int): Unit = {
    sessionMetrics.size.increment(count)
    if(poolMetricsEnabled) poolMetrics.size.increment(count)
  }

  def connectionClosed(): Unit = {
    sessionMetrics.size.decrement()
    if(poolMetricsEnabled) poolMetrics.size.decrement()
  }

  def error(): Unit = {
    sessionMetrics.errors.increment()
    if(poolMetricsEnabled) poolMetrics.errors.increment()
  }

  def retry(): Unit = {
    sessionMetrics.retries.increment()
  }

  def timeout(): Unit = {
    sessionMetrics.timeouts.increment()
    if(poolMetricsEnabled) poolMetrics.timeouts.increment()
  }

  def cancelation(): Unit = {
    sessionMetrics.canceled.increment()
    if(poolMetricsEnabled) poolMetrics.canceled.increment()
  }

  def speculativeExecution(): Unit = {
    sessionMetrics.speculations.increment()
    if(poolMetricsEnabled) poolMetrics.triggeredSpeculations.increment()
  }

  def executionStarted(): Unit = {
    sessionMetrics.inFlightRequests.increment()//TODO Exec endded on all others or?
  }
  def executionComplete(): Unit = {
    sessionMetrics.inFlightRequests.decrement()//TODO Exec endded on all others or?
  }

  def recordInFlightSample(value: Long): Unit = if(poolMetricsEnabled) poolMetrics.inFlight.record(value)

  def connectionTrashed(): Unit = {
    sessionMetrics.trashedConnections.increment()
    if(poolMetricsEnabled) poolMetrics.trashed.increment()
  }

  def recordBorrow(): Timer.Started = {
    val sessionTimer = sessionMetrics.borrow.start()
    val poolTimer = if(poolMetricsEnabled) Some(poolMetrics.borrow.start()) else None

    new Timer.Started {
      override def stop(): Unit = {
        sessionTimer.stop()
        poolTimer.foreach(_.stop())
      }
      override def withTag(key: String, value: String): Timer.Started = this
      override def withTag(key: String, value: Boolean): Timer.Started = this
      override def withTag(key: String, value: Long): Timer.Started = this
      override def withTags(tags: TagSet): Timer.Started = this
    }
  }
}
