package com.datastax.driver.core

import java.util.concurrent.atomic.AtomicInteger

import com.google.common.util.concurrent.{FutureCallback, ListenableFuture}
import kamon.Kamon
import kamon.instrumentation.cassandra.{HasPoolMetrics, HostPoolMetrics}
import kamon.instrumentation.cassandra.client.TargetResolver
import kanela.agent.libs.net.bytebuddy.asm.Advice


object PoolConstructorAdvice {
  @Advice.OnMethodExit
  def onConstructed(@Advice.This poolWithMetrics: HasPoolMetrics, @Advice.FieldValue("host") host: Host): Unit = {
    poolWithMetrics.setMetrics(new HostPoolMetrics(host))
  }
}

/*
* Measure time spent waiting for a connection
* Record number of inflight queries on just-aquired connection
* */
object BorrowAdvice {

  @Advice.OnMethodEnter
  def startBorrow: Long = {
    Kamon.clock.nanos
  }

  @Advice.OnMethodExit(suppress = classOf[Throwable])
  def onBorrowed(
                  @Advice.Return(readOnly = false) connection: ListenableFuture[Connection],
                  @Advice.Enter time: Long,
                  @Advice.This poolMetrics: HasPoolMetrics,
                  @Advice.FieldValue("totalInFlight") totalInflight: AtomicInteger): Unit = {

    val metrics = poolMetrics.getMetrics

    GuavaCompatibility.INSTANCE.addCallback(connection, new FutureCallback[Connection]() {

      private def duration(start: Long) = Kamon.clock.nanos - start

      override def onSuccess(borrowedConnection: Connection): Unit = {
        metrics.borrow.record(duration(time))
        metrics.inflightPerConnection.record(borrowedConnection.inFlight.get)
        metrics.inflightPerHost.record(totalInflight.get())
      }

      override def onFailure(t: Throwable): Unit = ()
    })
  }
}


/*
* Track number of active connections towards the given host
* Incremented when new connection requested and decremented either on
* connection being explicitly trashed or defunct
* */
object TrashConnectionAdvice {
  @Advice.OnMethodExit
  def onConnectionTrashed(@Advice.This hasPoolMetrics: HasPoolMetrics, @Advice.FieldValue("host") host: Host): Unit = {
    val metrics = hasPoolMetrics.getMetrics
    metrics.trashedConnections.increment()
    metrics.size.decrement()
  }
}

object CreateConnectionAdvice {
  @Advice.OnMethodExit
  def onConnectionCreated(@Advice.This hasPoolMetrics: HasPoolMetrics, @Advice.Return created: Boolean): Unit =
    if(created) {
      hasPoolMetrics.getMetrics.size.increment()
    }
}

object ConnectionDefunctAdvice {
  @Advice.OnMethodExit
  def onConnectionDefunct(@Advice.This hasPoolMetrics: HasPoolMetrics): Unit = {
    hasPoolMetrics.getMetrics.size.decrement()
  }
}

