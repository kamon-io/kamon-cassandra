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

import com.datastax.driver.core._
import kamon.instrumentation.cassandra.client.instrumentation.advisor.NewSessionMethodAdvisor
import kamon.instrumentation.context.HasContext.MixinWithInitializer
import kanela.agent.api.instrumentation.InstrumentationBuilder


class ClientInstrumentation extends InstrumentationBuilder {
  import kamon.instrumentation._

  onSubTypesOf("com.datastax.driver.core.Message$Response")
    .mixin(classOf[MixinWithInitializer])

  onType("com.datastax.driver.core.ArrayBackedResultSet$MultiPage")
    .mixin(classOf[MixinWithInitializer])

  onType("com.datastax.driver.core.Cluster$Manager")
    .advise(method("newSession"), classOf[NewSessionMethodAdvisor])

  onType("com.datastax.driver.core.HostConnectionPool")
    .advise(method("borrowConnection"), ConnectionPoolAdvice)

  onType("com.datastax.driver.core.ArrayBackedResultSet$MultiPage")
    .advise(method("queryNextPage"), OnFetchMore)

  onType("com.datastax.driver.core.ArrayBackedResultSet")
    .advise(method("fromMessage"), OnResultSetConstruction)

  //can be split into SpeculativeExecution(query,write) and general Connection.Callback (onXXX methods)
  onType("com.datastax.driver.core.RequestHandler$SpeculativeExecution")
    .mixin(classOf[MixinWithInitializer])
    .advise(method("query"), QueryExecutionAdvice)
    .advise(method("write"), QueryWriteAdvice)
    .advise(method("onException"), OnExceptionAdvice)
    .advise(method("onTimeout"), OnTimeoutAdvice)
    .advise(method("onSet"), OnSetAdvice)


}
