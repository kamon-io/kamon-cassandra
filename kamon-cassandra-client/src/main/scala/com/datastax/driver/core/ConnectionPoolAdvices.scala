package com.datastax.driver.core

import com.google.common.util.concurrent.{FutureCallback, ListenableFuture}
import kamon.Kamon
import kamon.instrumentation.cassandra.{HasPoolMetrics, HostPoolMetrics}
import kamon.instrumentation.cassandra.client.{TargetResolver}
import kanela.agent.libs.net.bytebuddy.asm.Advice


object PoolConstructorAdvice {
  @Advice.OnMethodExit
  def onConstructed(@Advice.This poolWithMetrics: HasPoolMetrics, @Advice.FieldValue("host") host: Host): Unit = {
    poolWithMetrics.set(new HostPoolMetrics(host))
  }
}

object BorrowAdvice {
  @Advice.OnMethodEnter
  def startBorrow: Long = {
    Kamon.clock.nanos
  }

  @Advice.OnMethodExit(suppress = classOf[Throwable])
  def onBorrowed(@Advice.Return(readOnly = false) connection: ListenableFuture[Connection], @Advice.Enter time: Long, @Advice.This poolMetrics: HasPoolMetrics): Unit = {
    GuavaCompatibility.INSTANCE.addCallback(connection, new FutureCallback[Connection]() {
      private def duration(start: Long) = Kamon.clock.nanos - start

      override def onSuccess(borrowedConnection: Connection): Unit = {
        poolMetrics.get.borrow.record(duration(time))
        poolMetrics.get.inflight.record(borrowedConnection.inFlight.get)
      }

      override def onFailure(t: Throwable): Unit = ()
    })
  }
}
