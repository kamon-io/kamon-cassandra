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

package kamon.cassandra

import java.nio.ByteBuffer
import java.util

import com.datastax.driver.core.{BoundStatement, RegularStatement, Statement}
import kamon.Kamon
import kamon.context.Context
import kamon.trace.Span

package object client {

  def attachSpanToStatement(clientSpan: Span, statement: Statement):Statement = {
    if(statement.isTracing) {
      val payload = new util.LinkedHashMap[String, ByteBuffer]()
      if(statement.getOutgoingPayload != null) payload.putAll(statement.getOutgoingPayload)
      payload.put("kamon-client-span", Kamon.contextCodec().Binary.encode(Context.create(Span.ContextKey, clientSpan)))
      statement.setOutgoingPayload(payload)
    }
    statement
  }

  def getQuery(statement: Statement):String =  statement match  {
    case b:BoundStatement => b.preparedStatement.getQueryString
    case r:RegularStatement => r.getQueryString
  }
}
