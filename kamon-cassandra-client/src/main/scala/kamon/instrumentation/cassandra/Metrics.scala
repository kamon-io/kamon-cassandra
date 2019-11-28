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

import java.net.InetAddress

import com.datastax.driver.core.Host
import kamon.Kamon
import kamon.instrumentation.cassandra.Cassandra.NodeTags
import kamon.metric._
import kamon.trace.Span



trait TargetResolver {
  def getTarget(address: InetAddress): String = address.getHostAddress
}

object PoolMetrics {
  val PoolBorrowTime      = Kamon.histogram("cassandra.client.pool.borrow-time", MeasurementUnit.time.nanoseconds)
  val ConnectionPoolSize  = Kamon.rangeSampler("cassandra.client.pool.size")
  val TrashedConnections  = Kamon.counter("cassandra.client.pool.trashed")
  val InFlightPerConnection = Kamon.histogram("cassandra.client.pool.inflight-per-connection")
}
class PoolMetrics(host: Host) extends TargetResolver  {
  val target = getTarget(host.getAddress)
  val borrow: Histogram = CassandraClientMetrics.poolBorrow(target)
  val size: RangeSampler = CassandraClientMetrics.connections(target)
  val trashedConnections = CassandraClientMetrics.trashedConnections(target)
  val inflightPerConnection: Histogram = CassandraClientMetrics.inflightPerConnection(target) //Number of requests inflight on this connect
  val inflightPerHost: Histogram = CassandraClientMetrics.inflightPerTarget(target)
}



object QueryMetrics {
  val Errors = Kamon.counter("cassandra.query.errors")
  val Timeouts = Kamon.counter("cassandra.query.timeouts")
  val RetriedExecutions =  Kamon.counter("cassandra.query.retries")
  val SpeculativeExecutions = Kamon.counter("cassandra.query.speculative")
  val CanceledExecutions = Kamon.counter("cassandra.query.cancelled")

  //TODO include config here
  def forHost(host: Host): QueryMetrics = new QueryMetrics(host)
}
//TODO bring in config
class QueryMetrics(host: Host) extends TargetResolver {
  import QueryMetrics._



  def tagSpanMetrics(span: Span, host: Host) = {
    //TODO need to tag span separately?
    if (Cassandra.config.nodeTags.node)
      span.tagMetrics("cassandra.node", getTarget(host.getAddress))
    if (Cassandra.config.nodeTags.dc)
      span.tagMetrics("cassandra.dc", host.getDatacenter)
    if (Cassandra.config.nodeTags.rack)
      span.tagMetrics("cassandra.rack", host.getRack)
  }

  def errors(host: String): Counter =
    Errors.withTag("target", host)

  def timeouts(host: String): Counter =
    Timeouts.withTag("target", host)

  /*Here it would be more valuable to tag with host that's being retried or speculated on than
  * one defined by a policy so we are dropping it altogether */
  def retries: Counter =
    RetriedExecutions.withoutTags()

  def speculative: Counter =
    SpeculativeExecutions.withoutTags()

  def cancelled: Counter =
    CanceledExecutions.withoutTags()

}


object CassandraClientMetrics {

  val DmlStatementPrefixes = Set("select", "insert", "update", "delete")


  //TODO done
  val PoolBorrowTime = Kamon.histogram("cassandra.client.pool-borrow-time", MeasurementUnit.time.nanoseconds)
  val ConnectionPoolSize = Kamon.rangeSampler("cassandra.connection-pool.size")
  val TrashedConnections = Kamon.counter("cassandra.trashed-connections")
  val InFlightPerConnection = Kamon.histogram("cassandra.client.inflight-per-connection") // Recorded at the moment of borrow, connection saturation

  val InFlightPerTarget = Kamon.histogram("cassandra.client.inflight-per-target")
  val QueryDuration = Kamon.histogram("cassandra.client.query.duration", MeasurementUnit.time.nanoseconds) //TODO from span metrics
  val ClientInflight = Kamon.rangeSampler("cassandra.client.inflight")

/*
  //TODO done
  val Errors = Kamon.counter("cassandra.query.errors")
  val Timeouts = Kamon.counter("cassandra.query.timeouts")
  val RetriedExecutions =  Kamon.counter("cassandra.query.retries")
  val SpeculativeExecutions = Kamon.counter("cassandra.query.speculative")
  val CanceledExecutions = Kamon.counter("cassandra.query.cancelled")*/

  class ConnectionMetrics(host: Host, nodeTags: NodeTags) {

  }


  def poolBorrow(host: String): Histogram =
      PoolBorrowTime.withTag("target", host)

  def connections(host: String): RangeSampler =
    ConnectionPoolSize.withTag("target", host)

  def trashedConnections(host: String): Counter =
    TrashedConnections.withTag("target", host)

  def inflightPerConnection(host: String): Histogram =
    InFlightPerConnection.withTag("target", host)

  def inflightPerTarget(host: String): Histogram =
    InFlightPerTarget.withTag("target", host)


  def queryDuration: Histogram =
    QueryDuration.withoutTags()


}