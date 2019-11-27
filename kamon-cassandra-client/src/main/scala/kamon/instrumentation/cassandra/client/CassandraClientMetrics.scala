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

package kamon.instrumentation.cassandra.client

import java.net.InetAddress
import java.util.concurrent.TimeUnit

import com.datastax.driver.core.{Host, Session}
import kamon.Kamon
import kamon.instrumentation.cassandra.Cassandra
import kamon.instrumentation.cassandra.Cassandra.NodeTags
import kamon.metric._
import kamon.tag.TagSet

import scala.util.Try


object TargetResolver {
  def getTarget(address: InetAddress): String = address.getHostAddress
}

object CassandraClientMetrics {

  val DmlStatementPrefixes = Set("select", "insert", "update", "delete")


  val PoolBorrowTime = Kamon.histogram("cassandra.client.pool-borrow-time", MeasurementUnit.time.nanoseconds)
  val ConnectinPoolSize = Kamon.histogram("cassandra.connection-pool.size")
  val TrashedConnections = Kamon.histogram("cassandra.trashed-connections")
  val InflightPerConnection = Kamon.histogram("cassandra.client.inflight-per-connection")
  val InflightPerTarget = Kamon.histogram("cassandra.client.inflight-per-target")
  val QueryDuration = Kamon.histogram("cassandra.client.query.duration", MeasurementUnit.time.nanoseconds)
  val QueryCount = Kamon.counter("cassandra.client.query.count")
  val ClientInflight = Kamon.rangeSampler("cassandra.client.inflight")

  val Errors = Kamon.counter("cassandra.query.errors")
  val Timeouts = Kamon.counter("cassandra.query.timeouts")
  val RetriedExecutions =  Kamon.counter("cassandra.query.retries")
  val SpeculativeExecutions = Kamon.counter("cassandra.query.speculative")
  val CanceledExecutions = Kamon.counter("cassandra.query.cancelled")

  class ConnectionMetrics(host: Host, nodeTags: NodeTags) {

  }


  def poolBorrow(host: String): Histogram =
      PoolBorrowTime.withTag("target", host)

  def connections(host: String): Histogram =
    ConnectinPoolSize.withTag("target", host)

  def trashedConnections(host: String): Histogram =
    TrashedConnections.withTag("target", host)

  def inflightPerConnection: Histogram =
    InflightPerConnection.withoutTags()

  def inflightDriver(host: String): Histogram =
    InflightPerTarget.withTag("target", host)


  def queryDuration: Histogram =
    QueryDuration.withoutTags()

  def queryCount: Counter =
    QueryCount.withoutTags()

  def queryInflight(host: String): RangeSampler =
    ClientInflight.withTag("target", host)





  def errors(host: String): Counter =
   Errors.withTag("target", host)

  def timeouts(host: String): Counter =
    Timeouts.withTag("target", host)

  /*Here it would be more valuable to tag with host that's being retried or speculated on than
  * one defined by a policy so we are dropping it altogether */`
  def retries: Counter =
   RetriedExecutions.withoutTags()

  def speculative: Counter =
    SpeculativeExecutions.withoutTags()

  def cancelled: Counter =
    CanceledExecutions.withoutTags()



  def recordQueryDuration(start: Long, end: Long, statementKind: Option[String]): Unit = {
    val statementTags = TagSet.of("statement.kind", statementKind.getOrElse("other"))
    queryDuration.withTags(statementTags).record(end - start)
    queryCount.withTags(statementTags).increment()
    queryInflight("ALL").decrement()
  }

  def from(session: Session): Unit = {
    import scala.collection.JavaConverters._

    Kamon.scheduler().scheduleAtFixedRate(new Runnable {
      override def run(): Unit = {
        val state = session.getState

        state.getConnectedHosts.asScala.foreach { host =>
          val hostId = TargetResolver.getTarget(host.getAddress)
          val trashed = state.getTrashedConnections(host)
          val openConnections = state.getOpenConnections(host)
          val inflightCount = state.getInFlightQueries(host)

          trashedConnections(hostId).record(trashed)
          inflightDriver(hostId).record(inflightCount)
          connections(hostId).record(openConnections)
        }
      }
    }, Cassandra.config.samplingIntervalMillis, Cassandra.config.samplingIntervalMillis, TimeUnit.MILLISECONDS)
  }
}