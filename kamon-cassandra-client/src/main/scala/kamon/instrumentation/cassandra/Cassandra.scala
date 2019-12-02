package kamon.instrumentation.cassandra

import com.datastax.driver.core.Host
import com.typesafe.config.Config
import kamon.Configuration.OnReconfigureHook
import kamon.Kamon
import kamon.instrumentation.trace.SpanTagger
import kamon.instrumentation.trace.SpanTagger.TagMode
import kamon.tag.TagSet
import kamon.trace.Span

object Cassandra {

  case class TargetNode(address: String, dc: String, rack: String)
  case class NodeTags(node: TagMode, rack: TagMode, dc: TagMode)
  case class ClientInstrumentationConfig(nodeTags: NodeTags)

  @volatile var config: ClientInstrumentationConfig = loadConfig(Kamon.config())

  private val UnknownTargetTagValue = "unknown"


  def loadConfig(config: Config) = ClientInstrumentationConfig(
    NodeTags(
      node = TagMode.from(config.getString("kamon.cassandra.tracing.tag.node")),
      rack = TagMode.from(config.getString("kamon.cassandra.tracing.tag.rack")),
      dc   = TagMode.from(config.getString("kamon.cassandra.tracing.tag.dc")),
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


  def targetMetricTags(target: TargetNode): TagSet = {
    val metricEnabledTags = Seq(
      ("target", target.address, config.nodeTags.node),
      ("dc", target.dc, config.nodeTags.dc),
      ("rack", target.rack, config.nodeTags.rack)
    )
      .filter(_._3 == TagMode.Metric)
      .map { case (tag, value, _) => tag -> value }
      .toMap

    TagSet.from(metricEnabledTags)
  }

  def tagSpanWithTarget(target: TargetNode, span: Span): Unit = {
    SpanTagger.tag(span, "target", target.address, config.nodeTags.node)
    SpanTagger.tag(span, "dc", target.dc, config.nodeTags.dc)
    SpanTagger.tag(span, "rack", target.rack, config.nodeTags.rack)
  }

}
