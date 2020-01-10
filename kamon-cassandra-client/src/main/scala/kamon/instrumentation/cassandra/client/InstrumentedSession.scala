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

package kamon.instrumentation.cassandra.client

import java.nio.ByteBuffer
import java.util

import com.datastax.driver.core._
import com.google.common.base.Function
import com.google.common.util.concurrent.{FutureCallback, Futures, ListenableFuture}
import kamon.Kamon
import kamon.instrumentation.cassandra.CassandraInstrumentation
import kamon.trace.Span

import scala.collection.JavaConverters._
import scala.util.{Failure, Success, Try}

object InstrumentedSession {
  val DmlStatementPrefixes = Set("select", "insert", "update", "delete")
}

class InstrumentedSession(underlying: Session) extends AbstractSession {
  import InstrumentedSession._

  override def getLoggedKeyspace: String =
    underlying.getLoggedKeyspace

  override def init(): Session =
    new InstrumentedSession(underlying.init())

  override def initAsync(): ListenableFuture[Session] = {
    Futures.transform(underlying.initAsync(), new Function[Session, Session] {
      override def apply(session: Session): Session =
        new InstrumentedSession(session)
    })
  }

  override def prepareAsync(
      query:         String,
      customPayload: util.Map[String, ByteBuffer]
  ): ListenableFuture[PreparedStatement] = {
    val statement = new SimpleStatement(query)
    statement.setOutgoingPayload(customPayload)
    underlying.prepareAsync(statement)
  }

  /** Try extracting type of a DML statement based on query string prefix.
    * It could be done matching on QueryBuilder statement subtypes but fails on SimpleStatements
    * http://cassandra.apache.org/doc/latest/cql/dml.html
    *
    * @param query query string
    * @return dml statement type, none if not a dml statement
    */
  def extractStatementType(query: String): Option[String] = {
    Option(query.substring(0, query.indexOf(" ")).toLowerCase)
      .filter(DmlStatementPrefixes.contains)
  }

  override def executeAsync(statement: Statement): ResultSetFuture = {
    val query         = getQuery(statement)
    val statementKind = extractStatementType(query)

    val clientSpan = Kamon
      .spanBuilder(QueryOperations.QueryOperationName)
      .tagMetrics("span.kind", "client")
      .tag("db.statement", query)
      .tag("db.type", "cassandra")
      .tagMetrics("cassandra.query.kind", statementKind.getOrElse("other"))
      .start

    Option(statement.getKeyspace).foreach(ks => clientSpan.tag("db.instance", ks))

    val future = Try(
      Kamon.runWithContext(Kamon.currentContext().withEntry(Span.Key, clientSpan)) {
        underlying.executeAsync(statement)
      }
    ) match {
      case Success(resultSetFuture) => resultSetFuture
      case Failure(cause) =>
        clientSpan.fail(cause.getMessage, cause)
        clientSpan.finish()
        throw cause
    }

    Futures.addCallback(
      future,
      new FutureCallback[ResultSet] {
        override def onSuccess(result: ResultSet): Unit = {
          recordClientQueryExecutionInfo(clientSpan, result)
          result.getExecutionInfo.getStatement match {
            case b: BoundStatement =>
              b.preparedStatement.getQueryString
            case r: RegularStatement =>
              r.getQueryString
          }
          clientSpan.finish()
        }

        override def onFailure(cause: Throwable): Unit = {
          clientSpan.fail(cause.getMessage, cause)
          clientSpan.finish()
        }
      }
    )
    future
  }

  private def recordClientQueryExecutionInfo(clientSpan: Span, result: ResultSet): Unit = {
    val info    = result.getExecutionInfo
    val hasMore = !result.isFullyFetched

    //does not invoke actual trace fetch
    val trace = info.getQueryTrace
    if (trace != null) {
      clientSpan.tag("cassandra.client.rs.session-id", trace.getTraceId.toString)
    }

    val cl = info.getAchievedConsistencyLevel
    if (cl != null) {
      clientSpan.tag("cassandra.client.rs.cl", cl.name())
    }

    clientSpan
      .tag("cassandra.client.rs.fetch-size", info.getStatement.getFetchSize)
      .tag("cassandra.client.rs.fetched", result.getAvailableWithoutFetching)
      .tag("cassandra.client.rs.has-more", hasMore)
  }

  override def closeAsync(): CloseFuture =
    underlying.closeAsync()

  override def isClosed: Boolean =
    underlying.isClosed

  override def getCluster: Cluster =
    underlying.getCluster

  override def getState: Session.State =
    underlying.getState

  def getQuery(statement: Statement): String = statement match {
    case b: BoundStatement =>
      b.preparedStatement.getQueryString
    case r: RegularStatement =>
      r.getQueryString
    case batchStatement: BatchStatement =>
      batchStatement.getStatements.asScala.map(getQuery).mkString(",")
    case _ => "unsupported-statement-type"
  }

}
