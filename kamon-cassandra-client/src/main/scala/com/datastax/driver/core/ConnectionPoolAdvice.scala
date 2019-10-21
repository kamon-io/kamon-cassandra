package com.datastax.driver.core

import com.google.common.util.concurrent.{FutureCallback, ListenableFuture}
import kamon.Kamon
import kamon.instrumentation.cassandra.client.{ClientMetrics, TargetResolver}
import kanela.agent.libs.net.bytebuddy.asm.Advice

object ConnectionPoolAdvice {

  @Advice.OnMethodEnter
  def startBorrow: Long = {
    Kamon.clock.nanos
  }

  @Advice.OnMethodExit(suppress = classOf[Throwable])
  def onBorrowed(@Advice.Return(readOnly = false) connection: ListenableFuture[Connection], @Advice.Enter time: Long): Unit = {
    GuavaCompatibility.INSTANCE.addCallback(connection, new FutureCallback[Connection]() {
      private def duration(start: Long) = Kamon.clock.nanos - start

      override def onSuccess(borrowedConnection: Connection): Unit = {
        val target = TargetResolver.getTarget(borrowedConnection.address.getAddress)
        ClientMetrics.poolBorrow(target).record(duration(time))
        ClientMetrics.inflightPerConnection.record(borrowedConnection.inFlight.get)
      }

      override def onFailure(t: Throwable): Unit = ()
    })
  }
}
