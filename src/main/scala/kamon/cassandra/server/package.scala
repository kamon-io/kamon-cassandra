package kamon.cassandra

import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.util

import kamon.Kamon
import kamon.context.{Context, TextMap}

package object server {
  private val UTF8 = Charset.forName("UTF-8")


  def decodeContextFrom(customPayload: util.Map[String, ByteBuffer]): Context = {
    Kamon.contextCodec().HttpHeaders.decode(readOnlyTextMapFromPayload(customPayload))
  }

  private def readOnlyTextMapFromPayload(customPayload: util.Map[String, ByteBuffer]): TextMap = new TextMap {
    import scala.collection.JavaConverters._

    private val headersMap = customPayload.asScala.map { case (k, v) => k -> (if (v != null) UTF8.decode(v).toString else "unknown")}.toMap

    override def values: Iterator[(String, String)] = headersMap.iterator
    override def get(key: String): Option[String] = headersMap.get(key)
    override def put(key: String, value: String): Unit = {}
  }
}
