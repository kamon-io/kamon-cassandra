package kamon.instrumentation.cassandra.metrics

import kamon.instrumentation.cassandra.CassandraInstrumentation
import kamon.instrumentation.cassandra.CassandraInstrumentation.Node
import kamon.instrumentation.cassandra.HostConnectionPoolMetrics.HostConnectionPoolInstruments
import kamon.instrumentation.cassandra.SessionMetrics.SessionInstruments
import kamon.metric.Timer
import kamon.trace.Span

class NodeMonitor(node: Node) {
  val sessionMetrics = new SessionInstruments(node)
  val poolMetrics = if(CassandraInstrumentation.settings.poolMetrics) {
    new HostConnectionPoolInstruments(node)
  } else null

  def tagSpan(span: Span): Unit = {
    CassandraInstrumentation.tagSpanWithNode(node, span)
  }

  def poolMetricsEnabled = poolMetrics != null

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

  def cancellation(): Unit = {
    sessionMetrics.canceled.increment()
    if(poolMetricsEnabled) poolMetrics.canceled.increment()
  }

  def speculativeExecution(): Unit = {
    sessionMetrics.speculations.increment()
    if(poolMetricsEnabled) poolMetrics.triggeredSpeculations.increment()
  }

  def executionStarted(): Unit = {
    sessionMetrics.inFlightRequests.increment()
  }

  def executionComplete(): Unit = {
    sessionMetrics.inFlightRequests.decrement()
  }

  def recordInFlightSample(value: Long): Unit = if(poolMetricsEnabled) poolMetrics.inFlight.record(value)

  def connectionTrashed(): Unit = {
    sessionMetrics.trashedConnections.increment()
  }

  def recordBorrow(nanos: Long): Timer.Started = {
    sessionMetrics.borrow.record(nanos)
    if(poolMetricsEnabled) poolMetrics.borrow.record(nanos)
  }
}
