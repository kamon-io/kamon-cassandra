package kamon.cassandra;

import kamon.Kamon;
import kamon.context.Context;
import kamon.context.Storage;
import kamon.trace.Span;
import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.experimental.NonFinal;
import org.apache.cassandra.tracing.TraceState;
import org.apache.cassandra.tracing.Tracing;

import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.UUID;


@Value
@EqualsAndHashCode(callSuper = false)
public class KamonTraceState extends TraceState {

    @NonFinal
    public Context context;

    @NonFinal
    public Span span;

    @NonFinal
    public Storage.Scope scope;

    public Map<String, ByteBuffer> customPayload;


    public KamonTraceState(InetAddress coordinator, UUID sessionId, Tracing.TraceType traceType, Map<String, ByteBuffer> customPayload) {
        super(coordinator, sessionId, traceType);
        this.customPayload = customPayload;
        this.context = Kamon.currentContext();
    }

    @Override
    protected void traceImpl(String message) {
        span.mark(message);
    }

    public void setScope(Storage.Scope scope) {
        this.scope = scope;
    }

    public void setSpan(Span span) {
        this.span = span;
    }
}
