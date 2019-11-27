package com.datastax.driver.core

import java.util.concurrent.atomic.AtomicReference

import com.datastax.driver.core.Message.Response
import com.datastax.driver.core.RequestHandler.QueryState
import kamon.{Kamon, trace}
import kamon.context.Context
import kamon.context.Storage.Scope
import kamon.instrumentation.cassandra.Cassandra
import kamon.instrumentation.cassandra.client.{CassandraClientMetrics, TargetResolver}
import kamon.instrumentation.context
import kamon.instrumentation.context.HasContext
import kamon.trace.Span
import kanela.agent.libs.net.bytebuddy.asm.Advice

object QueryOperations {
  val ExecutionPrefix = "cassandra.client.query"
  val QueryPrepareOperationName = ExecutionPrefix + ".prepare"
  val ExecutionOperationName = ExecutionPrefix + ".execution"
  val SpeculativeExecutionOperationName = ExecutionOperationName + ".speculative"
}

object QueryExecutionAdvice {
  import QueryOperations._

  val ParentSpanKey = Context.key[Span]("__parent-span", Span.Empty)

  @Advice.OnMethodEnter
  def onQueryExec(@Advice.This execution: HasContext,
                 @Advice.Argument(0) host: Host,
                 @Advice.FieldValue("position") position: Int,
                 @Advice.FieldValue("queryStateRef") queryState: AtomicReference[QueryState]): Unit = {
    val isSpeculative = position > 0
    val operationName = if(isSpeculative) SpeculativeExecutionOperationName else ExecutionOperationName

    if(isSpeculative) CassandraClientMetrics.speculative.increment()
    if(queryState.get().isCancelled) CassandraClientMetrics.cancelled.increment()

    val clientSpan = Kamon.currentSpan()
    val executionSpan = Kamon.spanBuilder(operationName).asChildOf(clientSpan).start()

    val executionContext = execution.context
      .withEntry(Span.Key, executionSpan)
      .withEntry(ParentSpanKey, clientSpan)

    execution.setContext(executionContext)

    executionSpan.tag("cassandra.node", TargetResolver.getTarget(host.getAddress))
    executionSpan.tag("cassandra.dc", host.getDatacenter)
    executionSpan.tag("cassandra.rack", host.getRack)

    tagExecutionMetrics(executionSpan, host)
  }

  private def tagExecutionMetrics(executionSpan: Span, host: Host) = {
    if(Cassandra.config.nodeTags.node)
      executionSpan.tagMetrics("cassandra.node", TargetResolver.getTarget(host.getAddress))
    if(Cassandra.config.nodeTags.dc)
      executionSpan.tagMetrics("cassandra.dc", host.getDatacenter)
    if(Cassandra.config.nodeTags.rack)
      executionSpan.tagMetrics("cassandra.rack", host.getRack)
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
                       @Advice.Return rs: ArrayBackedResultSet,
                       @Advice.Argument(0) msg: Responses.Result with HasContext,
                       ): Unit = if(rs.isInstanceOf[HasContext]) {
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
    val executionSpan = execution.context.get(Span.Key)
    executionSpan.mark("cassandra.connection.write-started")
  }
}

//Server timeouts and exceptions
object OnSetAdvice {
  import QueryOperations._

  @Advice.OnMethodEnter
  def onSetResult(@Advice.This execution: Connection.ResponseCallback with HasContext,
                 @Advice.Argument(1) response: Message.Response): Unit = {

    val executionSpan = execution.context.get(Span.Key)
    if(response.isInstanceOf[Responses.Result.Prepared]) executionSpan.name(QueryPrepareOperationName)
    if(execution.retryCount() > 0) {
      executionSpan.tag("retry", true)
      CassandraClientMetrics.retries.increment()
    }
    if(response.`type` == Response.Type.ERROR) executionSpan.fail(response.`type`.name())
    //In order to correlate paging requests with initial one, carry context with message
    response.asInstanceOf[HasContext].setContext(execution.context)
    executionSpan.finish()
  }
}

//Client exceptions
object OnExceptionAdvice {
  @Advice.OnMethodEnter
  def onException(@Advice.This execution: HasContext,
                  @Advice.Argument(0) connection: Connection,
                  @Advice.Argument(1) exception: Exception): Unit = {

    CassandraClientMetrics.errors(
      TargetResolver.getTarget(connection.address.getAddress)
    ).increment()

    val executionSpan = execution.context.get(Span.Key)
    executionSpan.fail(exception)
    executionSpan.finish()
    executionSpan.trackMetrics()
  }
}

//Client timeouts
object OnTimeoutAdvice {
  @Advice.OnMethodEnter
  def onTimeout(@Advice.This execution: HasContext,
                @Advice.Argument(0) connection: Connection): Unit = {

    CassandraClientMetrics.timeouts(
      TargetResolver.getTarget(connection.address.getAddress)
    ).increment()

    val executionSpan = execution.context.get(Span.Key)
    executionSpan.fail("timeout")
    executionSpan.finish()
  }
}
