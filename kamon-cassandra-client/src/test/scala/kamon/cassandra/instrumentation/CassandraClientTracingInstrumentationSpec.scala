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

import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.util

import com.datastax.driver.core.Session
import kamon.Kamon
import kamon.context.Context
import kamon.testkit.{MetricInspection, Reconfigure, TestSpanReporter}
import kamon.trace.Span
import kamon.trace.Span.TagValue
import kamon.util.Registration
import org.cassandraunit.utils.EmbeddedCassandraServerHelper
import org.scalatest.concurrent.Eventually
import org.scalatest.time.SpanSugar
import org.scalatest.{BeforeAndAfterAll, Matchers, OptionValues, WordSpec}

class CassandraClientTracingInstrumentationSpec extends WordSpec with Matchers with Eventually with SpanSugar with BeforeAndAfterAll
  with MetricInspection with Reconfigure with OptionValues {

  "the CassandraClientTracingInstrumentation" should {
    "does not generate Spans when tracing is disabled" in {
      session.execute(session.prepare("SELECT * FROM sync_test.users where name = 'alice' ALLOW FILTERING").bind())

      eventually(timeout(3 seconds)) {
        reporter.nextSpan() shouldBe None
      }
    }

    "generate Spans when tracing is enabled" in {
      session.execute(session.prepare("SELECT * FROM sync_test.users where name = 'alice' ALLOW FILTERING").enableTracing().bind())

      eventually(timeout(3 seconds)) {
        val span = reporter.nextSpan().value
//        span.operationName shouldBe "QUERY"
        span.tags("span.kind") shouldBe TagValue.String("client")
      }
    }

    "generate Spans when tracing is enabled and contains payload" in {
      val encodedSpan = Context.create(Span.ContextKey, Kamon.buildSpan("client-span").start())
      val payload = new util.LinkedHashMap[String, ByteBuffer]()
//      payload.put("kamon-client-span", Kamon.contextCodec().Binary.encode(encodedSpan))

      Kamon.withContext(encodedSpan) {
//        session.execute(session.prepare("SELECT * FROM sync_test.users where name = 'alice' ALLOW FILTERING").enableTracing().setOutgoingPayload(payload).bind())
        session.execute(session.prepare("SELECT * FROM sync_test.users where name = 'alice' ALLOW FILTERING").enableTracing().setOutgoingPayload(payload).bind())
      }

      eventually(timeout(3 seconds)) {
        val span = reporter.nextSpan().value
//        span.operationName shouldBe "QUERY"
        span.tags("span.kind") shouldBe TagValue.String("client")
//        span.tags("cassandra.query") shouldBe TagValue.String("SELECT * FROM sync_test.users where name = 'alice' ALLOW FILTERING")
      }
    }
  }


  private def encodeToByteBuffer(value:String):ByteBuffer = {
    Charset.forName("UTF-8").encode("463ac35c9f6413ad")
  }

  var registration: Registration = _
  var session:Session = _
  val reporter = new TestSpanReporter()

  override protected def beforeAll(): Unit = {
//    System.setProperty("cassandra.custom_tracing_class=kamon.cassandra.server.KamonTracing", classOf[KamonTracing].getName)

    EmbeddedCassandraServerHelper.startEmbeddedCassandra(40000L)
    enableFastSpanFlushing()
    sampleAlways()
    registration = Kamon.addReporter(reporter)
    session = EmbeddedCassandraServerHelper.getCluster.newSession()

    session.execute("DROP KEYSPACE IF EXISTS sync_test")
    session.execute("CREATE KEYSPACE sync_test WITH REPLICATION = {'class':'SimpleStrategy', 'replication_factor':3}")
    session.execute("CREATE TABLE   sync_test.users ( id UUID PRIMARY KEY, name text )")
    session.execute("INSERT INTO sync_test.users (id, name) values (uuid(), 'alice')")

  }

  override protected def afterAll(): Unit = {
    registration.cancel()
  }
}