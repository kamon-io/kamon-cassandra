package kamon.instrumentation.cassandra

import com.datastax.driver.core.Host
import com.typesafe.config.Config
import kamon.Configuration.OnReconfigureHook
import kamon.Kamon
import kamon.tag.TagSet

object Cassandra {

  case class TargetNode(address: String, dc: String, rack: String)
  case class NodeTags(node: Boolean, rack: Boolean, dc: Boolean)
  case class ClientInstrumentationConfig(samplingIntervalMillis: Long, nodeTags: NodeTags)

  @volatile var config: ClientInstrumentationConfig = loadConfig(Kamon.config())

  private val UnknownTargetTagValue = "unknown"


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

  def targetFromHost(host: Host): TargetNode = {
    TargetNode(
      host.getAddress.getHostAddress,
      Option(host.getDatacenter).getOrElse(UnknownTargetTagValue),
      Option(host.getRack).getOrElse(UnknownTargetTagValue)
    )
  }


  def targetTags(target: TargetNode): TagSet = {
    TagSet.from(
      Seq(
        Some("target" -> target.address).filter(_ => config.nodeTags.node),
        Some("dc" -> target.dc).filter(_ => config.nodeTags.dc),
        Some("rack" -> target.rack).filter(_ => config.nodeTags.rack)
      ).flatten.toMap
    )
  }

}
