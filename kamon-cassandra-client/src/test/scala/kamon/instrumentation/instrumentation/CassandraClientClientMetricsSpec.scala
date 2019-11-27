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
import kamon.instrumentation.cassandra.client.CassandraClientMetrics
import kamon.instrumentation.executor.ExecutorMetrics
import kamon.tag.TagSet
import kamon.testkit.{InstrumentInspection, MetricInspection}
import org.cassandraunit.utils.EmbeddedCassandraServerHelper
import org.scalatest.concurrent.Eventually
import org.scalatest.time.SpanSugar
import org.scalatest.{BeforeAndAfterAll, Matchers, OptionValues, WordSpec}

import scala.util.Try

class CassandraClientClientMetricsSpec extends WordSpec with Matchers with Eventually with SpanSugar with BeforeAndAfterAll
  with  MetricInspection.Syntax with InstrumentInspection.Syntax  with OptionValues {
  import CassandraClientMetrics._

  "the CassandraClientMetrics" should {

    "track client metrics" in {
      for(_ <- 1 to 100) yield {
        session.execute(session.prepare("SELECT * FROM kamon_cassandra_test.users where name = 'kamon' ALLOW FILTERING").bind())
      }

      val statementTags = TagSet.of("statement.kind", "select")

      eventually(timeout(3 seconds)) {
        poolBorrow("127.0.0.1").distribution(false).max shouldBe >= (1L)
        connections("127.0.0.1").distribution(false).max should be > 0L
        inflightPerConnection.distribution(false).max should be > 0L
        inflightDriver("127.0.0.1").distribution(false).max should be > 0L
        queryDuration.withTags(statementTags).distribution(false).max should be > 0L
        queryCount.withTags(statementTags).value(false) should be > 0L
        queryInflight("ALL").distribution().max should be > 0L

        errors("127.0.0.1").value(false) should equal (0)
        timeouts("127.0.0.1").value(false) should equal (0)
        retries.value(false) should equal (0)
        speculative.value(false) should equal (0)
        cancelled.value(false) should equal (0)
      }
    }

    "track the cassandra client executors queue size" in {
      for(_ <- 1 to 10) yield {
        session.executeAsync(session.prepare("SELECT * FROM kamon_cassandra_test.users where name = 'kamon' ALLOW FILTERING").bind())
      }

      eventually(timeout(10 seconds)) {
        val all = ExecutorMetrics.ThreadsTotal.instruments()
        all.map(_._2.distribution(false).max).forall(_ > 0) === true
      }
    }

  }

  var session:Session = _


  override protected def beforeAll(): Unit = {
    startCassandra()
  }

  private def startCassandra(): Unit = {
    EmbeddedCassandraServerHelper.startEmbeddedCassandra(40000L)
    //EmbeddedCassandraServerHelper.cleanEmbeddedCassandra()
    session = getSession
  }

  private def getSession: Session = {
    session = EmbeddedCassandraServerHelper.getCluster.newSession()

    session.execute("create keyspace kamon_cassandra_test with replication = {'class':'SimpleStrategy', 'replication_factor':3}")
    session.execute("create table kamon_cassandra_test.users (id uuid primary key, name text )")
    session.execute("insert into kamon_cassandra_test.users (id, name) values (uuid(), 'kamon')")
    session.execute("USE kamon_cassandra_test")
    session
  }

  override protected def afterAll(): Unit = {
    session.close()
  }
}