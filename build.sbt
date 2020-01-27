/* =========================================================================================
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

val kamonCore      = "io.kamon" %% "kamon-core"      % "2.0.4"
val kamonTestkit   = "io.kamon" %% "kamon-testkit"   % "2.0.4"
val kamonExecutors = "io.kamon" %% "kamon-executors" % "2.0.2"
val kanelaAgent     = "io.kamon" % "kanela-agent"                  % "1.0.4"
val kamonInstrument = "io.kamon" %% "kamon-instrumentation-common" % "2.0.0"

val cassandraDriver = "com.datastax.cassandra" % "cassandra-driver-core" % "3.6.0"
val cassandraAll    = "org.apache.cassandra"   % "cassandra-all"         % "3.11.2"
val cassandraUnit   = "org.cassandraunit"      % "cassandra-unit"        % "3.11.2.0"
val logbackCore = "ch.qos.logback" % "logback-core" % "1.2.3"

lazy val cassandraClient = (project in file("."))
  .enablePlugins(JavaAgent)
  .settings(instrumentationSettings)
  .settings(
    name := "kamon-cassandra",
    bintrayPackage := "kamon-cassandra",
    libraryDependencies ++=
      compileScope(kamonCore, kamonInstrument, kamonExecutors) ++
      providedScope(kanelaAgent, cassandraDriver) ++
      testScope(cassandraUnit, kamonTestkit, scalatest, slf4jApi, logbackClassic, logbackCore)
  )
