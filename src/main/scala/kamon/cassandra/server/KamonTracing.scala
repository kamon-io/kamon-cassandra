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
      println("KamonTracing-inside::newSession " + sessionId + " traceType " + traceType + " customPayload " + customPayload)
      val traceState  = new KamonTraceState(coordinator, sessionId, traceType, customPayload)

      set(traceState)
      sessions.put(sessionId, traceState)
      sessionId
    }
  }

  override def begin(request: String, client: InetAddress, parameters: util.Map[String, String]): TraceState = {
    println("KamonTracing::begin" + " request " + request + " client: " + client + " parameters " + parameters)
    val state = get().asInstanceOf[KamonTraceState]
    val incomingContext = decodeContextFrom(state.customPayload)

    val serverSpan = Kamon.buildSpan(state.traceType.name())
      .asChildOf(incomingContext.get(Span.ContextKey))
      .withMetricTag("span.kind", "server")
      .withTag("http.url", client.getHostAddress)
      .withTag("cassandra.request", request)
      .withTag("cassandra.session_id", state.sessionId.toString)
      .start()

    state.setScope(Kamon.storeContext(incomingContext.withKey(Span.ContextKey, serverSpan)))
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
