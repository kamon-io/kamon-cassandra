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
import com.google.common.base.{CaseFormat, Function}
import com.google.common.util.concurrent.{FutureCallback, Futures, ListenableFuture}
import kamon.Kamon
import kamon.context.BinaryPropagation.ByteStreamWriter
import kamon.context.Context
import kamon.trace.Span

import scala.util.{Failure, Success, Try}

class KamonSession(underlying: Session) extends AbstractSession {

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


  /** Try extracting type of a DML statement based on query string prefix.
    * It could be done matching on QueryBuilder statement subtypes but fails on SimpleStatements
    * http://cassandra.apache.org/doc/latest/cql/dml.html
    * @param query query string
    * @return dml statement type, none if not a dml statement
    */
  def extractStatementType(query: String): Option[String] = {
    Option(query.substring(0, query.indexOf(" ")).toLowerCase)
      .filter(CassandraClientMetrics.DmlStatementPrefixes.contains)
  }

  override def executeAsync(statement: Statement): ResultSetFuture = {
    val start = System.nanoTime()

    val query = getQuery(statement)
    val statementKind = extractStatementType(query)

    statement.getSerialConsistencyLevel

    val clientSpan = Kamon.spanBuilder("cassandra.client.query")
      .tagMetrics("span.kind", "client")
      .tag("db.statement", query)
      .tag("db.type", "cassandra")
      .start

    Option(statement.getKeyspace).foreach(ks => clientSpan.tag("db.instance", ks))
    statementKind.foreach(clientSpan.tagMetrics("cassandra.query.kind", _))


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

    CassandraClientMetrics.queryInflight("ALL").increment()

    Futures.addCallback(future, new FutureCallback[ResultSet] {
      override def onSuccess(result: ResultSet): Unit = {
        recordClientQueryExecutionInfo(clientSpan, result)
        result.getExecutionInfo.getStatement match  {
          case b:BoundStatement =>
            b.preparedStatement.getQueryString
          case r:RegularStatement =>
            r.getQueryString
        }

        CassandraClientMetrics.recordQueryDuration(start, System.nanoTime(), statementKind)
        clientSpan.finish()
      }

      override def onFailure(cause: Throwable): Unit = {
        CassandraClientMetrics.recordQueryDuration(start, System.nanoTime(), statementKind)
        clientSpan.fail(cause.getMessage, cause)
        clientSpan.finish()
      }
    })
    future
  }

  private def recordClientQueryExecutionInfo(clientSpan: Span, result: ResultSet): Unit = {
    val info = result.getExecutionInfo
    val hasMore = !result.isFullyFetched

    //does not invoke actual trace fetch
    Option(info.getQueryTrace).foreach { trace =>
      clientSpan.tag("cassandra.client.rs.session-id", trace.getTraceId.toString)
    }
    Option(info.getAchievedConsistencyLevel).foreach { CL =>
      clientSpan.tag("cassandra.client.rs.cl", CL.name())
    }

    clientSpan.tag("cassandra.client.rs.fetch-size", info.getStatement.getFetchSize)
    clientSpan.tag("cassandra.client.rs.fetched", result.getAvailableWithoutFetching)
    clientSpan.tag("cassandra.client.rs.has-more", hasMore)
  }

  override def closeAsync(): CloseFuture =
    underlying.closeAsync()

  override def isClosed: Boolean =
    underlying.isClosed

  override def getCluster: Cluster =
    underlying.getCluster

  override def getState: Session.State =
    underlying.getState


  def getQuery(statement: Statement):String =  statement match  {
    case b:BoundStatement => b.preparedStatement.getQueryString
    case r:RegularStatement => r.getQueryString
  }


  private class BBBackedByteStreamWriter extends ByteStreamWriter {
    val underlying = ByteBuffer.allocate(16)
    override def write(bytes: Array[Byte]): Unit = underlying.put(bytes)
    override def write(bytes: Array[Byte], offset: Int, count: Int): Unit = underlying.put(bytes, offset, count)
    override def write(byte: Int): Unit = underlying.put(byte.toByte)
  }
}
