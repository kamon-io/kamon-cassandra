package kamon.instrumentation.cassandra

import com.typesafe.config.Config
import kamon.Configuration.OnReconfigureHook
import kamon.Kamon

object Cassandra {
  case class NodeTags(node: Boolean, rack: Boolean, dc: Boolean)
  case class ClientInstrumentationConfig(samplingIntervalMillis: Long, nodeTags: NodeTags)

  @volatile var config: ClientInstrumentationConfig = loadConfig(Kamon.config())

  def loadConfig(config: Config) = ClientInstrumentationConfig(
    config.getDuration("kamon.cassandra.sample-interval").toMillis,
    NodeTags(
      node = config.getBoolean("kamon.cassandra.tracing.tag.node"),
      rack = config.getBoolean("kamon.cassandra.tracing.tag.rack"),
      dc   = config.getBoolean("kamon.cassandra.tracing.tag.dc"),
    )
  )

  Kamon.onReconfigure(new OnReconfigureHook {
    override def onReconfigure(newConfig: Config): Unit =
      config = loadConfig(newConfig)
  })

}
