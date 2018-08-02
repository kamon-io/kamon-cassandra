/*
 * =========================================================================================
 * Copyright © 2013-2018 the kamon project <http://kamon.io/>
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
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

package com.datastax.driver.core

import kamon.cassandra.Metrics.ExecutorQueueMetrics

object ExecutorQueueMetricsExtractor {
  def from(session: Session, executorQueueMetrics: ExecutorQueueMetrics):Unit = {
    val manager = session.getCluster.manager
    executorQueueMetrics.executorQueueDepth.set(manager.executorQueue.size())
    executorQueueMetrics.blockingQueueDepth.set(manager.blockingExecutorQueue.size())
    executorQueueMetrics.reconnectionTaskCount.set(manager.reconnectionExecutorQueue.size())
    executorQueueMetrics.taskSchedulerTaskCount.set(manager.scheduledTasksExecutorQueue.size())
  }
}
