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

package kamon.instrumentation.cassandra


import com.datastax.driver.core.Host
import kamon.Kamon
import kamon.instrumentation.cassandra.Cassandra.TargetNode
import kamon.metric._
import kamon.trace.Span


object PoolMetrics {
  val PoolBorrowTime        = Kamon.histogram("cassandra.client.pool.borrow-time", MeasurementUnit.time.nanoseconds)
  val ConnectionPoolSize    = Kamon.rangeSampler("cassandra.client.pool.size")
  val TrashedConnections    = Kamon.counter("cassandra.client.pool.trashed")
  val InFlightPerConnection = Kamon.histogram("cassandra.client.pool.inflight-per-connection")
  val InFlightPerTarget     = Kamon.histogram("cassandra.client.inflight-per-target")
}

class PoolMetrics(node: TargetNode) {
  import PoolMetrics._

  val targetTags = Cassandra.targetTags(node)

  val borrow: Histogram                 = PoolBorrowTime.withTags(targetTags)
  val size: RangeSampler                = ConnectionPoolSize.withTags(targetTags)
  val trashedConnections: Counter       = TrashedConnections.withTags(targetTags)
  val inflightPerConnection: Histogram  = InFlightPerConnection.withTags(targetTags)
  val inflightPerHost: Histogram        = InFlightPerTarget.withTags(targetTags)
}


object QueryMetrics {
  val Errors                = Kamon.counter("cassandra.query.errors")
  val Timeouts              = Kamon.counter("cassandra.query.timeouts")
  val RetriedExecutions     = Kamon.counter("cassandra.query.retries")
  val SpeculativeExecutions = Kamon.counter("cassandra.query.speculative")
  val CanceledExecutions    = Kamon.counter("cassandra.query.cancelled")

  def forHost(host: Host): QueryMetrics = new QueryMetrics(Cassandra.targetFromHost(host))
}

class QueryMetrics(targetNode: TargetNode) {
  import QueryMetrics._
  private val targetTags = Cassandra.targetTags(targetNode)

  def tagSpanMetrics(span: Span): Span = span.tagMetrics(targetTags)

  /*Here it would be more valuable to tag with host that's being retried or speculated on than
  * one defined by a policy so we are dropping it altogether */
  def retries: Counter                = RetriedExecutions.withoutTags()
  def speculative: Counter            = SpeculativeExecutions.withoutTags()
  def cancelled: Counter              = CanceledExecutions.withoutTags()

  def errors: Counter   = Errors.withTags(targetTags)
  def timeouts: Counter = Timeouts.withTags(targetTags)
}





