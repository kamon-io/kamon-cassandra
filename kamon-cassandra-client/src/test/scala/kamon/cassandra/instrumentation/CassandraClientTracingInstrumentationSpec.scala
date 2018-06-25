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

package kamon.cassandra.instrumentation

import com.datastax.driver.core.Session
import kamon.Kamon
import kamon.context.Context
import kamon.testkit.{MetricInspection, Reconfigure, TestSpanReporter}
import kamon.trace.Span.TagValue
import kamon.trace.{Span, SpanCustomizer}
import kamon.util.Registration
import org.cassandraunit.utils.EmbeddedCassandraServerHelper
import org.scalatest.concurrent.Eventually
import org.scalatest.time.SpanSugar
import org.scalatest.{BeforeAndAfterAll, Matchers, OptionValues, WordSpec}

class CassandraClientTracingInstrumentationSpec extends WordSpec with Matchers with Eventually with SpanSugar with BeforeAndAfterAll
  with MetricInspection with Reconfigure with OptionValues {

  "the CassandraClientTracingInstrumentation" should {
    "does not generate Spans when tracing is disabled" in {
      session.execute(session.prepare("SELECT * FROM kamon_cassandra_test.users where name = 'kamon' ALLOW FILTERING").bind())

      eventually(timeout(3 seconds)) {
        reporter.nextSpan() shouldBe None
      }
    }

    "generate Spans when tracing is enabled" in {
      val encodedSpan = Context.create(Span.ContextKey, Kamon.buildSpan("client-span").start())
      Kamon.withContext(encodedSpan) {
        session.execute(session.prepare("SELECT * FROM kamon_cassandra_test.users where name = 'kamon' ALLOW FILTERING").enableTracing().bind())
      }

      eventually(timeout(3 seconds)) {
        val span = reporter.nextSpan().value
        span.operationName shouldBe "bound-statement"
        span.tags("span.kind") shouldBe TagValue.String("client")
        span.tags("cassandra.query") shouldBe TagValue.String("SELECT * FROM kamon_cassandra_test.users where name = 'kamon' ALLOW FILTERING")
      }
    }

   "pickup a SpanCustomizer from the current context and apply it to the new spans" in {
     Kamon.withContext(Context(SpanCustomizer.ContextKey, SpanCustomizer.forOperationName("client-span"))) {
        session.execute(session.prepare("SELECT * FROM kamon_cassandra_test.users where name = 'kamon' ALLOW FILTERING").enableTracing().bind())
     }

     eventually(timeout(3 seconds)) {
       val span = reporter.nextSpan().value
       span.operationName shouldBe "client-span"
       span.tags("span.kind") shouldBe TagValue.String("client")
       span.tags("cassandra.query") shouldBe TagValue.String("SELECT * FROM kamon_cassandra_test.users where name = 'kamon' ALLOW FILTERING")
     }
   }
  }

  var registration: Registration = _
  var session:Session = _
  val reporter = new TestSpanReporter()

  override protected def beforeAll(): Unit = {
    EmbeddedCassandraServerHelper.startEmbeddedCassandra(40000L)
    enableFastSpanFlushing()
    sampleAlways()
    registration = Kamon.addReporter(reporter)
    session = EmbeddedCassandraServerHelper.getCluster.newSession()

    session.execute("drop keyspace if exists kamon_cassandra_test")
    session.execute("create keyspace kamon_cassandra_test with replication = {'class':'SimpleStrategy', 'replication_factor':3}")
    session.execute("create table kamon_cassandra_test.users (id uuid primary key, name text )")
    session.execute("insert into kamon_cassandra_test.users (id, name) values (uuid(), 'kamon')")

  }

  override protected def afterAll(): Unit = {
    registration.cancel()
  }
}