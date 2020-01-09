package com.datastax.driver.core

import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

import com.google.common.util.concurrent.{FutureCallback, ListenableFuture}
import kamon.Kamon
import kamon.instrumentation.cassandra.metrics.{HasPoolMetrics, MetricProxy, PoolWithMetrics}
import kamon.instrumentation.cassandra.CassandraInstrumentation
import kamon.metric.Timer
import kanela.agent.libs.net.bytebuddy.asm.Advice


object PoolConstructorAdvice {
  @Advice.OnMethodExit
  def onConstructed(@Advice.This poolWithMetrics: HostConnectionPool with HasPoolMetrics,
                    @Advice.FieldValue("host") host: Host,
                    @Advice.FieldValue("totalInFlight") totalInflight: AtomicInteger): Unit = {
    val clusterName = poolWithMetrics.manager.getCluster.getClusterName
    val node = CassandraInstrumentation.targetFromHost(host, clusterName)
    val samplingInterval = CassandraInstrumentation.settings.sampleInterval.toMillis

    poolWithMetrics.setMetrics(new MetricProxy(node))

    val samplingSchedule = Kamon.scheduler().scheduleAtFixedRate(new Runnable {
      override def run(): Unit = {
        poolWithMetrics.getMetrics.recordInFlightSample(totalInflight.longValue())
      }
    }, samplingInterval, samplingInterval, TimeUnit.MILLISECONDS)

    poolWithMetrics.setSampling(samplingSchedule)
  }
}

object PoolCloseAdvice {
  @Advice.OnMethodExit
  def onClose(@Advice.This poolWithMetrics: HostConnectionPool with HasPoolMetrics): Unit = {
    Option(poolWithMetrics.getSampling).foreach(_.cancel(true))
  }
}

/*
* Measure time spent waiting for a connection
* Record number of inflight queries on just-aquired connection
* */
object BorrowAdvice {

  @Advice.OnMethodEnter
  def startBorrow(@Advice.This poolMetrics: HasPoolMetrics): Timer.Started = {
    poolMetrics.getMetrics.recordBorrow
  }

  @Advice.OnMethodExit(suppress = classOf[Throwable])
  def onBorrowed(
                  @Advice.Return(readOnly = false) connection: ListenableFuture[Connection],
                  @Advice.Enter timer: Timer.Started,
                  @Advice.This poolMetrics: HasPoolMetrics,
                  @Advice.FieldValue("totalInFlight") totalInflight: AtomicInteger): Unit = {

    GuavaCompatibility.INSTANCE.addCallback(connection, new FutureCallback[Connection]() {
      override def onSuccess(borrowedConnection: Connection): Unit = {
        timer.stop()
      }
      override def onFailure(t: Throwable): Unit = timer.stop() //TODO failure count, should it mix with succeseful borrows
    })
  }
}


/*
* Track number of active connections towards the given host
* Incremented when new connection requested and decremented either on
* connection being explicitly trashed or defunct
* */
object InitPoolAdvice {
  @Advice.OnMethodExit
  def onPoolInited(
                    @Advice.This hasPoolMetrics: HasPoolMetrics,
                    @Advice.Return done: ListenableFuture[_],
                  @Advice.FieldValue("open") openConnections: AtomicInteger): Unit = {

    done.addListener(new Runnable {
      override def run(): Unit = {
        hasPoolMetrics.getMetrics.connectionsOpened(openConnections.get())
      }
    }, Kamon.scheduler())
  }
}

object CreateConnectionAdvice {
  @Advice.OnMethodExit
  def onConnectionCreated(@Advice.This hasPoolMetrics: HasPoolMetrics, @Advice.Return created: Boolean): Unit =
    if (created) {
      hasPoolMetrics.getMetrics.connectionsOpened(1)
    }
}

object TrashConnectionAdvice {
  @Advice.OnMethodExit
  def onConnectionTrashed(@Advice.This hasPoolMetrics: HasPoolMetrics, @Advice.FieldValue("host") host: Host): Unit = {
    hasPoolMetrics.getMetrics.connectionTrashed
  }
}


object ConnectionDefunctAdvice {
  @Advice.OnMethodExit
  def onConnectionDefunct(@Advice.This hasPoolMetrics: HasPoolMetrics): Unit = {
    hasPoolMetrics.getMetrics.connectionClosed
  }
}

