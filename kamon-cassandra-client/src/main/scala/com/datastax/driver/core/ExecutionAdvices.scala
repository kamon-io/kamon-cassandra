package com.datastax.driver.core

import java.util.concurrent.atomic.AtomicReference

import com.datastax.driver.core.Message.Response
import com.datastax.driver.core.RequestHandler.QueryState
import kamon.Kamon
import kamon.context.Context
import kamon.context.Storage.Scope
import kamon.instrumentation.cassandra.CassandraInstrumentation
import kamon.instrumentation.cassandra.client.ClientInstrumentation.ClusterManagerBridge
import kamon.instrumentation.cassandra.metrics.{HasPoolMetrics, NodeMonitor}
import kamon.instrumentation.context.HasContext
import kamon.trace.Span
import kanela.agent.libs.net.bytebuddy.asm.Advice

object QueryOperations {
  val QueryOperationName = "query"
  val QueryPrepareOperationName: String = QueryOperationName + ".prepare"
  val ExecutionOperationName:    String = QueryOperationName + ".execution"
}

object QueryExecutionAdvice {
  import QueryOperations._

  val ParentSpanKey: Context.Key[Span] = Context.key[Span]("__parent-span", Span.Empty)

  @Advice.OnMethodEnter
  def onQueryExec(
      @Advice.This execution:                         HasContext,
      @Advice.Argument(0) host:                       Host with HasPoolMetrics,
      @Advice.FieldValue("position") position:        Int,
      @Advice.FieldValue("queryStateRef") queryState: AtomicReference[QueryState]
  ): Unit = {
    val metrics = host.getMetrics

    val clientSpan = Kamon.currentSpan()
    val executionSpan = Kamon
      .clientSpanBuilder(ExecutionOperationName, "cassandra.client")
      .asChildOf(clientSpan)
      .start()

    val isSpeculative = position > 0
    if (isSpeculative) {
      metrics.speculativeExecution()
      executionSpan.tag("cassandra.speculative", true)
    }
    if (queryState.get().isCancelled) metrics.cancellation()
    host.getMetrics.executionStarted()

    metrics.tagSpan(executionSpan)

    val executionContext = execution.context
      .withEntry(Span.Key, executionSpan)
      .withEntry(ParentSpanKey, clientSpan)

    execution.setContext(executionContext)
  }
}

/*
 * Transfer context from msg to created result set so it can be used
 * for further page fetches
 *
 * */
object OnResultSetConstruction {
  @Advice.OnMethodExit
  def onCreateResultSet(
      @Advice.Return rs:       ArrayBackedResultSet,
      @Advice.Argument(0) msg: Responses.Result with HasContext
  ): Unit = if (rs.isInstanceOf[HasContext]) {
    rs.asInstanceOf[HasContext].setContext(msg.context)
  }

}

object OnFetchMore {
  @Advice.OnMethodEnter
  def onFetchMore(@Advice.This hasContext: HasContext): Scope = {
    val clientSpan = hasContext.context.get(QueryExecutionAdvice.ParentSpanKey)
    Kamon.storeContext(Context.of(Span.Key, clientSpan))
  }
  @Advice.OnMethodExit
  def onFetched(@Advice.Enter scope: Scope): Unit = {
    scope.close()
  }
}

object QueryWriteAdvice {
  @Advice.OnMethodEnter
  def onStartWriting(@Advice.This execution: HasContext): Unit = {
    execution.context
      .get(Span.Key)
      .mark("cassandra.connection.write-started")
  }
}

//Server timeouts and exceptions
object OnSetAdvice {
  import QueryOperations._

  @Advice.OnMethodEnter
  def onSetResult(
      @Advice.This execution:                    Connection.ResponseCallback with HasContext,
      @Advice.Argument(0) connection:            Connection,
      @Advice.Argument(1) response:              Message.Response,
      @Advice.FieldValue("current") currentHost: Host with HasPoolMetrics
  ): Unit = {

    val executionSpan = execution.context.get(Span.Key)
    if (response.isInstanceOf[Responses.Result.Prepared])
      executionSpan.name(QueryPrepareOperationName)
    if (execution.retryCount() > 0) {
      executionSpan.tag("cassandra.retry", true)
      currentHost.getMetrics.retry()
    }
    if (response.`type` == Response.Type.ERROR) {
      executionSpan.fail(response.`type`.name())
      currentHost.getMetrics.serverError()
    }

    currentHost.getMetrics.executionComplete()
    //In 7   order to correlate paging requests with initial one, carry context with message
    response.asInstanceOf[HasContext].setContext(execution.context)
    executionSpan.finish()
  }
}

//Client exceptions
object OnExceptionAdvice {
  @Advice.OnMethodEnter
  def onException(
      @Advice.This execution:                    HasContext,
      @Advice.Argument(0) connection:            Connection,
      @Advice.Argument(1) exception:             Exception,
      @Advice.FieldValue("current") currentHost: Host with HasPoolMetrics
  ): Unit = {
    currentHost.getMetrics.clientError()
    currentHost.getMetrics.executionComplete()
    execution.context
      .get(Span.Key)
      .fail(exception)
      .finish()
  }
}

//Client timeouts
object OnTimeoutAdvice {
  @Advice.OnMethodEnter
  def onTimeout(
      @Advice.This execution:                    HasContext,
      @Advice.Argument(0) connection:            Connection,
      @Advice.FieldValue("current") currentHost: Host with HasPoolMetrics
  ): Unit = {
    currentHost.getMetrics.timeout()
    currentHost.getMetrics.executionComplete()
    execution.context
      .get(Span.Key)
      .fail("timeout")
      .finish()
  }
}

object HostLocationAdvice {
  @Advice.OnMethodExit
  def onHostLocationUpdate(
      @Advice.This host:                            Host with HasPoolMetrics,
      @Advice.FieldValue("manager") clusterManager: Any
  ): Unit = {
    val targetHost = CassandraInstrumentation.nodeFromHost(
      host,
      clusterManager.asInstanceOf[ClusterManagerBridge].getClusterName
    )
    host.setMetrics(new NodeMonitor(targetHost))
  }
}
