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

package kamon.cassandra.server

import java.net.InetAddress
import java.nio.ByteBuffer
import java.util
import java.util.UUID

import kamon.Kamon
import kamon.cassandra.KamonTraceState
import kamon.trace.Span
import org.apache.cassandra.tracing.Tracing.TraceType
import org.apache.cassandra.tracing.{TraceState, Tracing}
import org.apache.cassandra.utils.FBUtilities

class KamonTracing extends Tracing {
  private val coordinator = FBUtilities.getLocalAddress

  Kamon.loadReportersFromConfig()

  override def newSession(sessionId: UUID, traceType: TraceType, customPayload: util.Map[String, ByteBuffer]): UUID = {
    if(traceType == TraceType.NONE) super.newSession(sessionId, traceType, customPayload)
    else {
      val traceState  = new KamonTraceState(coordinator, sessionId, traceType, customPayload)

      set(traceState)
      sessions.put(sessionId, traceState)
      sessionId
    }
  }

  override def begin(request: String, client: InetAddress, parameters: util.Map[String, String]): TraceState = {
    val state = get().asInstanceOf[KamonTraceState]
    val clientSpan = decodeSpanFrom(state.customPayload)

    val serverSpan = Kamon.buildSpan(state.traceType.name())
      .asChildOf(clientSpan)
      .withMetricTag("span.kind", "server")
      .withTag("http.url", client.getHostAddress)
      .withTag("cassandra.request", request)
      .withTag("cassandra.query", parameters.getOrDefault("query", "unknown-query"))
      .withTag("cassandra.session_id", state.sessionId.toString)
      .start()

    state.setScope(Kamon.storeContext(Kamon.currentContext().withKey(Span.ContextKey, serverSpan)))
    state.setSpan(serverSpan)
    state
  }

  override def stopSessionImpl(): Unit = {
    val state = get().asInstanceOf[KamonTraceState]

    if (state != null) {
      state.scope.close()
      state.span.finish()
    }
  }

  override def newTraceState(coordinator: InetAddress, sessionId: UUID, traceType: Tracing.TraceType): TraceState =
    throw new AssertionError()

  override def trace(sessionId: ByteBuffer, message: String, ttl: Int): Unit = {}
}
