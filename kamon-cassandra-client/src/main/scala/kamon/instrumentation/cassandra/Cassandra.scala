package kamon.instrumentation.cassandra

import com.typesafe.config.Config
import kamon.Configuration.OnReconfigureHook
import kamon.Kamon

object Cassandra {
  @volatile var samplingIntervalMillis: Long = samplingIntervalFromConfig(Kamon.config())

  Kamon.onReconfigure(new OnReconfigureHook {
    override def onReconfigure(newConfig: Config): Unit =
      samplingIntervalMillis = samplingIntervalFromConfig(newConfig)
  })

  def samplingIntervalFromConfig(config: Config): Long =
    config.getDuration("kamon.cassandra.sample-interval").toMillis
}
