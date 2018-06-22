/* =========================================================================================
 * Copyright © 2013-2018 the kamon project <http://kamon.io/>
 *
 * Licensed under the Apache License, Version 2.0 (the "License") you may not use this file
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

package kamon.cassandra.client

import java.nio.ByteBuffer
import java.util

import com.datastax.driver.core._
import com.google.common.base.Function
import com.google.common.util.concurrent.{FutureCallback, Futures, ListenableFuture}
import kamon.Kamon

class TracingSession(underlying: Session) extends AbstractSession {

  override def getLoggedKeyspace: String =
    underlying.getLoggedKeyspace

  override def init(): Session =
    new TracingSession(underlying.init())


  override def initAsync(): ListenableFuture[Session] = {
    Futures.transform(underlying.initAsync(), new Function[Session, Session] {
      override def apply(session: Session): Session =
        new TracingSession(session)
    })
  }

  override def prepareAsync(query: String, customPayload: util.Map[String, ByteBuffer]): ListenableFuture[PreparedStatement] = {
    val statement = new SimpleStatement(query)
    statement.setOutgoingPayload(customPayload)
    prepareAsync(statement)
  }

  override def prepareAsync(query: String): ListenableFuture[PreparedStatement] =
    underlying.prepareAsync(query)


  override def executeAsync(statement: Statement): ResultSetFuture = {
    val clientSpan = Kamon.buildSpan(getQuery(statement))
      .withMetricTag("span.kind", "client")
      .withTag("http.url", underlying.getCluster.getClusterName)
      .withTag("cassandra.query", getQuery(statement))
      .withTag("cassandra.keyspace", statement.getKeyspace)
      .start()

      val statementWitSpan = attachSpanToStatement(clientSpan, statement)

      val future = underlying.executeAsync(statementWitSpan)

      Futures.addCallback(future ,new FutureCallback[ResultSet] {
        override def onSuccess(result: ResultSet): Unit =
          clientSpan.finish()

        override def onFailure(t: Throwable): Unit = {
          clientSpan.addError(t.getMessage, t)
          clientSpan.finish()
        }
      })
    future
  }

  override def closeAsync(): CloseFuture =
    underlying.closeAsync()

  override def isClosed: Boolean =
    underlying.isClosed

  override def getCluster: Cluster =
    underlying.getCluster

  override def getState: Session.State =
    underlying.getState
}
