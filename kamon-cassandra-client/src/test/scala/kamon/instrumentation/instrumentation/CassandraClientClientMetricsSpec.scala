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

import com.datastax.driver.core.{Cluster, Session}
import kamon.instrumentation.cassandra.client.ClientMetrics
import kamon.testkit.{InstrumentInspection, MetricInspection}
import org.cassandraunit.utils.EmbeddedCassandraServerHelper
import org.scalatest.concurrent.Eventually
import org.scalatest.time.SpanSugar
import org.scalatest.{BeforeAndAfterAll, Matchers, OptionValues, WordSpec}

import scala.util.Try

class CassandraClientClientMetricsSpec extends WordSpec with Matchers with Eventually with SpanSugar with BeforeAndAfterAll
  with  MetricInspection.Syntax with InstrumentInspection.Syntax  with OptionValues {

  "the CassandraClientMetrics" should {

    "track the total of active requests in ALL hosts" in {
      for(_ <- 1 to 100) yield {
        session.execute(session.prepare("SELECT * FROM kamon_cassandra_test.users where name = 'kamon' ALLOW FILTERING").bind())
      }

      eventually(timeout(3 seconds)) {
        ClientMetrics.queryInflight("ALL").distribution(false).max shouldBe >= (1L)
      }
    }

    "track the total of active requests" in {
      for(_ <- 1 to 100) yield {
        session.execute(session.prepare("SELECT * FROM kamon_cassandra_test.users where name = 'kamon' ALLOW FILTERING").bind())
      }

      eventually(timeout(3 seconds)) {
        ClientMetrics.queryInflight("127.0.0.1").distribution(false).max shouldBe > (0L)

      }
    }
    /*
        "track the query count" in {
          for(_ <- 1 to 100) yield {
            session.execute(session.prepare("SELECT * FROM kamon_cassandra_test.users where name = 'kamon' ALLOW FILTERING").bind())
          }

          eventually(timeout(3 seconds)) {
            Metrics.queryCount.value() shouldBe >= (100L)
          }
        }

        "track the query duration" in {
          for(_ <- 1 to 100) yield {
            session.execute(session.prepare("SELECT * FROM kamon_cassandra_test.users where name = 'kamon' ALLOW FILTERING").bind())
          }

          eventually(timeout(3 seconds)) {
            Metrics.queryDuration.distribution().count shouldBe >= (100L)
          }
        }

        "track the cassandra client executors queue size" in {
          for(_ <- 1 to 100) yield {
            session.execute(session.prepare("SELECT * FROM kamon_cassandra_test.users where name = 'kamon' ALLOW FILTERING").bind())
          }

          eventually(timeout(3 seconds)) {
            val metrics = Metrics.ExecutorQueueMetrics()
            metrics.executorQueueDepth.value() shouldBe >= (0L)
            metrics.blockingQueueDepth.value() shouldBe >= (0L)
            metrics.reconnectionTaskCount.value() shouldBe >= (0L)
            metrics.taskSchedulerTaskCount.value() shouldBe >= (0L)
          }
        }*/
  }

  var session:Session = _


  override protected def beforeAll(): Unit = {
    Try(startCassandra()) //will fail if reruning tests in same JVM, no way of checking if its running already :/
    getSession.foreach(s => session = s)
  }

  private def startCassandra(): Unit = {
    EmbeddedCassandraServerHelper.startEmbeddedCassandra(40000L)
  }

  private def getSession: Try[Session] = Try {
    session = EmbeddedCassandraServerHelper.getCluster.newSession()
    session.execute("drop keyspace if exists kamon_cassandra_test")
    session.execute("create keyspace kamon_cassandra_test with replication = {'class':'SimpleStrategy', 'replication_factor':3}")
    session.execute("create table kamon_cassandra_test.users (id uuid primary key, name text )")
    session.execute("insert into kamon_cassandra_test.users (id, name) values (uuid(), 'kamon')")
    session
  }

  override protected def afterAll(): Unit = {
    session.close()
  }
}