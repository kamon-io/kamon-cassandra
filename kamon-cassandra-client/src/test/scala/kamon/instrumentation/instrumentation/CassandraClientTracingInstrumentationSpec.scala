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

package kamon.instrumentation.instrumentation

import com.datastax.driver.core.{QueryOperations, Session}
import com.datastax.driver.core.querybuilder.QueryBuilder
import kamon.Kamon
import kamon.module.Module.Registration
import kamon.testkit.{InstrumentInspection, MetricInspection, Reconfigure, TestSpanReporter}
import org.cassandraunit.utils.EmbeddedCassandraServerHelper
import org.scalatest.concurrent.Eventually
import org.scalatest.time.SpanSugar
import org.scalatest.{BeforeAndAfterAll, Matchers, OptionValues, WordSpec}
import kamon.tag.Lookups._


class CassandraClientTracingInstrumentationSpec extends WordSpec with Matchers with Eventually with SpanSugar with BeforeAndAfterAll
  with  MetricInspection.Syntax with InstrumentInspection.Syntax  with Reconfigure with OptionValues  with TestSpanReporter {

  "the CassandraClientTracingInstrumentation" should {

    "trace query prepare" in {
      session.prepare("SELECT * FROM kamon_cassandra_test.users where name = 'kamon' ALLOW FILTERING")
      eventually(timeout(10 seconds)) {
        testSpanReporter().nextSpan().map(_.operationName) shouldBe Some(QueryOperations.QueryPrepareOperationName)
      }
    }

    //execution, retry, speculation, prepare
    // dc, target
    //has a writing mark
    //timeout
    //fetch size, hasMore, retrieved


    "trace executions separately from user invocation" in {

    }

    //session.execute(session.prepare("SELECT * FROM kamon_cassandra_test.users where name = 'kamon' ALLOW FILTERING").bind())
  }

  var registration: Registration = _
  var session:Session = _

  override protected def beforeAll(): Unit = {
    EmbeddedCassandraServerHelper.startEmbeddedCassandra(40000L)
    enableFastSpanFlushing()
    sampleAlways()
    session = EmbeddedCassandraServerHelper.getCluster.newSession()

    session.execute("drop keyspace if exists kamon_cassandra_test")
    session.execute("create keyspace kamon_cassandra_test with replication = {'class':'SimpleStrategy', 'replication_factor':3}")
    session.execute("create table kamon_cassandra_test.users (id uuid primary key, name text )")
    session.execute("insert into kamon_cassandra_test.users (id, name) values (uuid(), 'kamon')")

  }

  override protected def afterAll(): Unit = {
//    registration.cancel()
  }
}