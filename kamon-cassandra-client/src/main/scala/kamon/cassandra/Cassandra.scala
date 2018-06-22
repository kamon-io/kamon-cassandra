/* =========================================================================================
 * Copyright © 2013-2017 the kamon project <http://kamon.io/>
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

package kamon.cassandra

import java.util.concurrent.TimeUnit

import com.typesafe.config.Config
import kamon.{Kamon, OnReconfigureHook}
import kamon.util.DynamicAccess
import org.slf4j.LoggerFactory

object Cassandra {
  private val logger = LoggerFactory.getLogger(Cassandra.getClass)
//  @volatile private var slowQueryThresholdMicroseconds: Long = 2000000
//  @volatile private var slowQueryProcessor: SlowQueryProcessor = new SlowQueryProcessor.Default
//  @volatile private var sqlErrorProcessor: SqlErrorProcessor = new SqlErrorProcessor.Default

  loadConfiguration(Kamon.config())

  Kamon.onReconfigure(new OnReconfigureHook {
    override def onReconfigure(newConfig: Config): Unit =
      Cassandra.loadConfiguration(newConfig)
  })


  private def loadConfiguration(config: Config): Unit = {
    try {
      val jdbcConfig = config.getConfig("kamon.cassandra")
      val dynamic = new DynamicAccess(getClass.getClassLoader)

    } catch {
      case t: Throwable => logger.error("The kamon-cassandra module failed to load configuration", t)
    }
  }
}