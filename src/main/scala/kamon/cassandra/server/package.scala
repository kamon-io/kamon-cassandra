/* =========================================================================================
 * Copyright © 2013-2018 the kamon project <http://kamon.io/>
 *
 * Licensed under the Apache License, Version 2.0 (the "License") you may not use this file
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

package kamon.cassandra

import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.util

import kamon.Kamon
import kamon.context.{Context, TextMap}

package object server {
  private val UTF8 = Charset.forName("UTF-8")

  def decodeContextFrom(customPayload: util.Map[String, ByteBuffer]): Context = {
    if(customPayload == null || customPayload.isEmpty) Context.Empty
    else Kamon.contextCodec().HttpHeaders.decode(readOnlyTextMapFromPayload(customPayload))
  }

  private def readOnlyTextMapFromPayload(customPayload: util.Map[String, ByteBuffer]): TextMap = new TextMap {
    import scala.collection.JavaConverters._

    private val headersMap = customPayload.asScala.map { case (k, v) => k -> (if (v != null) UTF8.decode(v).toString else "unknown")}.toMap

    override def values: Iterator[(String, String)] = headersMap.iterator
    override def get(key: String): Option[String] = headersMap.get(key)
    override def put(key: String, value: String): Unit = {}
  }
}
