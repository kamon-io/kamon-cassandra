package kamon.instrumentation.cassandra.executors

import java.util.concurrent.{Callable, ExecutorService, ScheduledExecutorService}

import kamon.instrumentation.executor.ExecutorInstrumentation
import kamon.tag.TagSet
import kanela.agent.api.instrumentation.InstrumentationBuilder
import kanela.agent.libs.net.bytebuddy.implementation.bind.annotation.SuperCall

class DriverExecutorInstrumentation extends InstrumentationBuilder {

  /*Wraps all internaly created executors with Kamon-instrumented ones*/
  onType("com.datastax.driver.core.ThreadingOptions")
    .intercept(method("createExecutor"), CreateExecutorAdvice)
    .intercept(method("createBlockingExecutor"), CreateBlockingTasksExecutorAdvice)
    .intercept(method("createReaperExecutor"), CreateReaperExecutorAdvice)
    .intercept(method("createScheduledTasksExecutor"), CreateScheduledTasksExecutorAdvice)
    .intercept(method("createReconnectionExecutor"), CreateReconnectionExecutorAdvice)
}

trait ExecutorMetrics {

  def metricName(executorName: String) = "cassandra.client.executor." + executorName
  val componentTags = TagSet.of("component", "cassandra.client")

  def instrument(callable: Callable[ExecutorService], name: String): ExecutorService =
    ExecutorInstrumentation.instrument(
      callable.call(),
      metricName(name),
      componentTags
    )

  def instrumentScheduled(
      callable: Callable[ScheduledExecutorService],
      name:     String
  ): ScheduledExecutorService =
    ExecutorInstrumentation.instrumentScheduledExecutor(
      callable.call(),
      metricName(name),
      componentTags
    )
}

object CreateExecutorAdvice extends ExecutorMetrics {
  def onExecutorCreated(@SuperCall callable: Callable[ExecutorService]): ExecutorService =
    instrument(callable, "executor")
}

object CreateBlockingTasksExecutorAdvice extends ExecutorMetrics {
  def onExecutorCreated(@SuperCall callable: Callable[ExecutorService]): ExecutorService =
    instrument(callable, "blocking")
}

object CreateReaperExecutorAdvice extends ExecutorMetrics {
  def onExecutorCreated(
      @SuperCall callable: Callable[ScheduledExecutorService]
  ): ScheduledExecutorService =
    instrumentScheduled(callable, "reaper")
}

object CreateScheduledTasksExecutorAdvice extends ExecutorMetrics {
  def onExecutorCreated(
      @SuperCall callable: Callable[ScheduledExecutorService]
  ): ScheduledExecutorService =
    instrumentScheduled(callable, "scheduled-tasks")
}

object CreateReconnectionExecutorAdvice extends ExecutorMetrics {
  def onExecutorCreated(
      @SuperCall callable: Callable[ScheduledExecutorService]
  ): ScheduledExecutorService =
    instrumentScheduled(callable, "reconnection")
}
