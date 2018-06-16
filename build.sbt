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


val kamonCore           = "io.kamon"            %% "kamon-core"               % "1.1.2"
val kamonTestkit        = "io.kamon"            %% "kamon-testkit"            % "1.1.2"
val scalaExtension      = "io.kamon"            %% "kanela-scala-extension"   % "0.0.10"

val cassandraDriver     = "com.datastax.cassandra"    % "cassandra-driver-core"	    % "3.5.0"
val cassandraAll        = "org.apache.cassandra"      % "cassandra-all"        	    % "3.11.2"
val cassandraUnit	      = "org.cassandraunit"         % "cassandra-unit"       	    % "3.5.0.1"
val lombok              = "org.projectlombok"         % "lombok"                    % "1.18.0"


lazy val root = (project in file("."))
  .enablePlugins(JavaAgent)
  .settings(name := "kamon-cassandra")
  .settings(javaAgents += "io.kamon"    % "kanela-agent"   % "0.0.300"  % "compile;test")
  .settings(resolvers += Resolver.bintrayRepo("kamon-io", "snapshots"))
  .settings(resolvers += Resolver.mavenLocal)
  .settings(
      libraryDependencies ++=
        compileScope(kamonCore, cassandraDriver, scalaExtension) ++
        providedScope(lombok, cassandraAll) ++
        testScope(cassandraUnit, kamonTestkit, scalatest, slf4jApi, logbackClassic))
