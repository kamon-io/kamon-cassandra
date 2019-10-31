package com.datastax.driver.core

import kamon.instrumentation.cassandra.client.ClientMetrics.ExecutorQueueMetrics

object ExecutorQueueMetricsExtractor {
  def from(session: Session, executorQueueMetrics: ExecutorQueueMetrics): Unit = {
    val manager = session.getCluster.manager

    Option(manager.executorQueue).foreach { q =>
      executorQueueMetrics.executorQueueDepth.update(q.size())
    }
    Option(manager.blockingExecutorQueue).foreach { q =>
      executorQueueMetrics.blockingQueueDepth.update(q.size())
    }
    Option(manager.reconnectionExecutorQueue).foreach { q =>
      executorQueueMetrics.reconnectionTaskCount.update(q.size())
    }
    Option(manager.scheduledTasksExecutorQueue).foreach { q =>
      executorQueueMetrics.taskSchedulerTaskCount.update(q.size())
    }
  }
}
