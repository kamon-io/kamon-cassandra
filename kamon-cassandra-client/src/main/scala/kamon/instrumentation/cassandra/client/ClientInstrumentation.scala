/*
 * =========================================================================================
 * Copyright © 2013-2018 the kamon project <http://kamon.io/>
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
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

package kamon.instrumentation.cassandra.client

import java.util.UUID

import com.datastax.driver.core._
import kamon.instrumentation.cassandra.client.ClientInstrumentation.ClusterManagerBridge
import kamon.instrumentation.cassandra.metrics.PoolWithMetrics
import kamon.instrumentation.context.HasContext.MixinWithInitializer
import kanela.agent.api.instrumentation.InstrumentationBuilder
import kanela.agent.api.instrumentation.bridge.FieldBridge


object ClientInstrumentation {
  trait ClusterManagerBridge {
    @FieldBridge("clusterName")
    def getClusterName: String
  }
}

class ClientInstrumentation extends InstrumentationBuilder {

  import kamon.instrumentation._

  /*Wapps client session with traced Kamon one*/
  onType("com.datastax.driver.core.Cluster$Manager")
    .intercept(method("newSession"), SessionInterceptor)
    .bridge(classOf[ClusterManagerBridge])

  /*Instrument  connection pools (one per target host)
  * Pool size is incremented on pool init and when new connections are added
  * and decremented when connection is deemed defunct or explicitly trashed.
  * Pool metrics are mixed in the pool object itself*/
  onType("com.datastax.driver.core.HostConnectionPool")
    .advise(method("borrowConnection"), BorrowAdvice)
    .advise(method("trashConnection"), TrashConnectionAdvice)
    .advise(method("addConnectionIfUnderMaximum"), CreateConnectionAdvice)
    .advise(method("onConnectionDefunct"), ConnectionDefunctAdvice)
    .advise(isConstructor, PoolConstructorAdvice)
    .advise(method("initAsync"), InitPoolAdvice)
    .advise(method("closeAsync"), PoolCloseAdvice)
    .mixin(classOf[PoolWithMetrics])

  /*Trace each query sub-execution as a child of client query,
  * this includes retries, speculative executions and fetchMore executions.
  * Once response is ready (onSet), context is carried via Message.Response mixin
  * to be used for further fetches*/
  onType("com.datastax.driver.core.RequestHandler$SpeculativeExecution")
    .advise(method("query"), QueryExecutionAdvice)
    .advise(method("write"), QueryWriteAdvice)
    .advise(method("onException"), OnExceptionAdvice)
    .advise(method("onTimeout"), OnTimeoutAdvice)
    .advise(method("onSet"), OnSetAdvice)
    .mixin(classOf[MixinWithInitializer])

  onSubTypesOf("com.datastax.driver.core.Message$Response")
    .mixin(classOf[MixinWithInitializer])

  onType("com.datastax.driver.core.ArrayBackedResultSet")
    .advise(method("fromMessage"), OnResultSetConstruction)


  /*In order for fetchMore execution to be a sibling of original execution
  * we need to carry parent-span id through result sets */
  onType("com.datastax.driver.core.ArrayBackedResultSet$MultiPage")
    .mixin(classOf[MixinWithInitializer])
  onType("com.datastax.driver.core.ArrayBackedResultSet$MultiPage")
    .advise(method("queryNextPage"), OnFetchMore)

  /*Query metrics are tagged with target information (based on config)
  * so all query metrics are mixed into a Host object*/
  onType("com.datastax.driver.core.Host")
    .mixin(classOf[PoolWithMetrics])
    .advise(method("setLocationInfo"), HostLocationAdvice)

}

