package kamon.instrumentation.cassandra

import com.datastax.driver.core.Host
import com.typesafe.config.Config
import kamon.Configuration.OnReconfigureHook
import kamon.Kamon
import kamon.instrumentation.trace.SpanTagger
import kamon.instrumentation.trace.SpanTagger.TagMode
import kamon.tag.TagSet
import kamon.trace.Span

object CassandraInstrumentation {

  case class TargetNode(address: String, dc: String, rack: String)
  case class Settings(node: TagMode, rack: TagMode, dc: TagMode)

  @volatile var settings: Settings = loadConfig(Kamon.config())

  private val UnknownTargetTagValue = "unknown"

  def loadConfig(config: Config) = Settings(
    node = TagMode.from(config.getString("kamon.cassandra.tracing.tag.node")),
    rack = TagMode.from(config.getString("kamon.cassandra.tracing.tag.rack")),
    dc   = TagMode.from(config.getString("kamon.cassandra.tracing.tag.dc"))
  )

  Kamon.onReconfigure(new OnReconfigureHook {
    override def onReconfigure(newConfig: Config): Unit =
      settings = loadConfig(newConfig)
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
      ("cassandra.target", target.address, settings.node),
      ("cassandra.dc", target.dc, settings.dc),
      ("cassandra.rack", target.rack, settings.rack)
    )
      .filter(_._3 == TagMode.Metric)
      .map { case (tag, value, _) => tag -> value }
      .toMap

    TagSet.from(metricEnabledTags)
  }

  def tagSpanWithTarget(target: TargetNode, span: Span): Unit = {
    SpanTagger.tag(span, "cassandra.target", target.address, settings.node)
    SpanTagger.tag(span, "cassandra.dc", target.dc, settings.dc)
    SpanTagger.tag(span, "cassandra.rack", target.rack, settings.rack)
  }

}
