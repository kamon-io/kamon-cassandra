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
import kamon.cassandra.Metrics
import kamon.trace.{Span, SpanCustomizer}

import scala.util.{Failure, Success, Try}

class KamonSession(underlying: Session) extends AbstractSession {

  Metrics.from(underlying)

  override def getLoggedKeyspace: String =
    underlying.getLoggedKeyspace

  override def init(): Session =
    new KamonSession(underlying.init())


  override def initAsync(): ListenableFuture[Session] = {
    Futures.transform(underlying.initAsync(), new Function[Session, Session] {
      override def apply(session: Session): Session =
        new KamonSession(session)
    })
  }

  override def prepareAsync(query: String, customPayload: util.Map[String, ByteBuffer]): ListenableFuture[PreparedStatement] = {
    val statement = new SimpleStatement(query)
    statement.setOutgoingPayload(customPayload)
    underlying.prepareAsync(statement)
  }

  override def executeAsync(statement: Statement): ResultSetFuture = {
    val start = System.nanoTime()
    val currentContext = Kamon.currentContext()
    val parentSpan = currentContext.get(Span.ContextKey)

    val clientSpanBuilder = Kamon.buildSpan(getSpanName(statement))
      .asChildOf(parentSpan)
      .withMetricTag("span.kind", "client")
      .withTag("http.url", underlying.getCluster.getClusterName)
      .withTag("cassandra.query", getQuery(statement))
      .withTag("cassandra.keyspace", statement.getKeyspace)

    val clientSpan = currentContext.get(SpanCustomizer.ContextKey)
      .customize(clientSpanBuilder)
      .start()

    val statementWithSpan = attachSpanToStatement(clientSpan, statement)

    val future = Try(underlying.executeAsync(statementWithSpan)) match {
      case Success(resultSetFuture) => resultSetFuture
      case Failure(cause) =>
        clientSpan.addError(cause.getMessage, cause)
        clientSpan.finish()
        throw cause
    }

    Metrics.inflight("ALL")

    Futures.addCallback(future, new FutureCallback[ResultSet] {
      override def onSuccess(result: ResultSet): Unit = {
        Metrics.recordQueryDuration(start, System.nanoTime())
        clientSpan.finish()
      }

      override def onFailure(cause: Throwable): Unit = {
        Metrics.recordQueryDuration(start, System.nanoTime())
        clientSpan.addError(cause.getMessage, cause)
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
