package com.datastax.driver.core

object ClusterNameExtractor {
  def unsafeGetNameFromManager(clusterManager: AnyRef): String =
    clusterManager.getClass
      .getDeclaredField("clusterName")
      .get(clusterManager)
      .asInstanceOf[String]
}
