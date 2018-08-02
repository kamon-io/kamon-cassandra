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
