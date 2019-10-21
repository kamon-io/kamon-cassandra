package com.datastax.driver.core

import kamon.instrumentation.cassandra.client.ClientMetrics.ExecutorQueueMetrics

object ExecutorQueueMetricsExtractor {
  def from(session: Session, executorQueueMetrics: ExecutorQueueMetrics):Unit = {
    val manager = session.getCluster.manager
    executorQueueMetrics.executorQueueDepth.update(manager.executorQueue.size())
    executorQueueMetrics.blockingQueueDepth.update(manager.blockingExecutorQueue.size())
    executorQueueMetrics.reconnectionTaskCount.update(manager.reconnectionExecutorQueue.size())
    executorQueueMetrics.taskSchedulerTaskCount.update(manager.scheduledTasksExecutorQueue.size())
  }
}
