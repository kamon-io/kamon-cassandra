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

package kamon.cassandra

import com.typesafe.config.Config
import kamon.{Kamon, OnReconfigureHook}

object Cassandra {
  @volatile var samplingIntervalMillis: Long = samplingIntervalFromConfig(Kamon.config())

  Kamon.onReconfigure(new OnReconfigureHook {
    override def onReconfigure(newConfig: Config): Unit =
      samplingIntervalMillis = samplingIntervalFromConfig(newConfig)
  })

  def samplingIntervalFromConfig(config: Config): Long =
    config.getDuration("kamon.cassandra.sample-interval").toMillis
}