/*
 * =========================================================================================
 * Copyright © 2013-2018 the kamon project <http://kamon.io/>
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 * =========================================================================================
 */

package kamon.cassandra

import java.util.concurrent.TimeUnit

import com.datastax.driver.core.{ExecutorQueueMetricsExtractor, Session}
import kamon.Kamon
import kamon.cassandra.Cassandra.samplingIntervalMillis
import kamon.metric._

object Metrics {

  def inflight(host: String): RangeSampler =
    Kamon.rangeSampler("cassandra.client-inflight").refine("target", host)

  def inflightDriver(host: String): Histogram =
    Kamon.histogram("cassandra.client-inflight-driver").refine("target", host)

  def queryDuration: Histogram =
    Kamon.histogram("cassandra.query-duration", MeasurementUnit.time.nanoseconds)

  def queryCount: Counter =
    Kamon.counter("cassandra.query-count")

  def connections(host: String): Histogram =
    Kamon.histogram("cassandra.connection-pool-size").refine("target", host)

  def trashedConnections(host: String): Histogram =
    Kamon.histogram("cassandra.trashed-connections").refine("target", host)

  def recordQueryDuration(start: Long, end: Long): Unit = {
    queryDuration.record(end - start)
    queryCount.increment(1)
    inflight("ALL").decrement()
  }

  def from(session: Session): Unit = {
    import scala.collection.JavaConverters._

    Kamon.scheduler().scheduleAtFixedRate(() => {
      val state = session.getState

      ExecutorQueueMetricsExtractor.from(session, ExecutorQueueMetrics())

      state.getConnectedHosts.asScala.foreach { host =>
        val hostId = host.getAddress.getHostAddress
        val trashed = state.getTrashedConnections(host)
        val openConnections = state.getOpenConnections(host)
        val inflightCount = state.getInFlightQueries(host)

        session.getCluster.getMetrics.getRegistry.getCounters()
        trashedConnections(hostId).record(trashed)
        inflightDriver(hostId).record(inflightCount)
        connections(hostId).record(openConnections)
      }
    }, samplingIntervalMillis, samplingIntervalMillis, TimeUnit.MILLISECONDS)
  }


  case class ExecutorQueueMetrics(executorQueueDepth: Gauge,
                                  blockingQueueDepth: Gauge,
                                  reconnectionTaskCount: Gauge,
                                  taskSchedulerTaskCount: Gauge)

  object ExecutorQueueMetrics {
    def apply(): ExecutorQueueMetrics = {
      val generalTags = Map("component" -> "cassandra-client")
      new ExecutorQueueMetrics(
        Kamon.gauge("executor-queue-depth").refine(generalTags),
        Kamon.gauge("blocking-executor-queue-depth").refine(generalTags),
        Kamon.gauge("reconnection-scheduler-task-count").refine(generalTags),
        Kamon.gauge("task-scheduler-task-count").refine(generalTags))
    }
  }

}