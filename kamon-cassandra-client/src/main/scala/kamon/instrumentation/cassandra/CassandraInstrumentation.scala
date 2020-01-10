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

  object Tags {
    val Host = "cassandra.host"
    val DC = "cassandra.dc"
    val Rack = "cassandra.rack"
    val Cluster = "cassandra.cluster"
    val ErrorSource = "source"
  }


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

  def allTags(node: Node): TagSet =
    TagSet.from(
      Map(
        Tags.Host -> node.address,
        Tags.DC -> node.dc,
        Tags.Rack -> node.rack,
        Tags.Cluster -> node.cluster
      )
    )


  def nodeMetricTags(node: Node): TagSet = {
    val metricEnabledTags = Seq(
      (Tags.Host, node.address, settings.host),
      (Tags.DC, node.dc, settings.dc),
      (Tags.Rack, node.rack, settings.rack),
      (Tags.Cluster, node.cluster, settings.cluster)
    )
      .filter(_._3 == TagMode.Metric)
      .map { case (tag, value, _) => tag -> value }
      .toMap

    TagSet.from(metricEnabledTags)
  }

  def tagSpanWithNode(node: Node, span: Span): Unit = {
    SpanTagger.tag(span, Tags.Host, node.address, settings.host)
    SpanTagger.tag(span, Tags.DC, node.dc, settings.dc)
    SpanTagger.tag(span, Tags.Rack, node.rack, settings.rack)
    SpanTagger.tag(span, Tags.Cluster, node.cluster, settings.cluster)
  }

}
