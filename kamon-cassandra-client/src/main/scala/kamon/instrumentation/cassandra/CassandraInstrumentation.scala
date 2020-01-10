package kamon.instrumentation.cassandra

import com.datastax.driver.core.Host
import com.typesafe.config.Config
import kamon.Configuration.OnReconfigureHook
import kamon.Kamon
import kamon.instrumentation.cassandra.metrics.NodeMonitor
import kamon.instrumentation.trace.SpanTagger
import kamon.instrumentation.trace.SpanTagger.TagMode
import kamon.tag.TagSet
import kamon.trace.Span

import scala.concurrent.duration.Duration

object CassandraInstrumentation {

  case class Node(address: String, dc: String, rack: String, cluster: String)
  case class Settings(sampleInterval: Duration, poolMetrics: Boolean, host: TagMode, rack: TagMode, dc: TagMode, cluster: TagMode)

  @volatile var settings: Settings = loadConfig(Kamon.config())

  private val UnknownTargetTagValue = "unknown"

  def loadConfig(config: Config) = Settings(
    sampleInterval = Duration.fromNanos(
      config.getDuration("kamon.cassandra.sample-interval").toNanos
    ),
    poolMetrics = config.getBoolean("kamon.cassandra.track-pool-metrics"),
    host = TagMode.from(config.getString("kamon.cassandra.tracing.tag.host")),
    rack = TagMode.from(config.getString("kamon.cassandra.tracing.tag.rack")),
    dc   = TagMode.from(config.getString("kamon.cassandra.tracing.tag.dc")),
    cluster   = TagMode.from(config.getString("kamon.cassandra.tracing.tag.cluster"))
  )

  Kamon.onReconfigure(new OnReconfigureHook {
    override def onReconfigure(newConfig: Config): Unit =
      settings = loadConfig(newConfig)
  })

  def nodeFromHost(host: Host, cluster: String): Node = {
    Node(
      host.getAddress.getHostAddress,
      Option(host.getDatacenter).getOrElse(UnknownTargetTagValue),
      Option(host.getRack).getOrElse(UnknownTargetTagValue),
      cluster
    )
  }

  def nodeMetricTags(node: Node): TagSet = {
    val metricEnabledTags = Seq(
      ("cassandra.host", node.address, settings.host), //TODO move to internal tag dictionary
      ("cassandra.dc", node.dc, settings.dc),
      ("cassandra.rack", node.rack, settings.rack),
      ("cassandra.cluster", node.cluster, settings.cluster)
    )
      .filter(_._3 == TagMode.Metric)
      .map { case (tag, value, _) => tag -> value }
      .toMap

    TagSet.from(metricEnabledTags)
  }

  def tagSpanWithNode(node: Node, span: Span): Unit = {
    SpanTagger.tag(span, "cassandra.host", node.address, settings.host)
    SpanTagger.tag(span, "cassandra.dc", node.dc, settings.dc)
    SpanTagger.tag(span, "cassandra.rack", node.rack, settings.rack)
    SpanTagger.tag(span, "cassandra.cluster", node.cluster, settings.cluster)
  }

}
