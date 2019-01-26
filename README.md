Cassandra Integration   ![Build Status](https://travis-ci.org/kamon-io/kamon-cassandra.svg?branch=master)
==========================

[![Gitter](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/kamon-io/Kamon?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.kamon/kamon-cassandra_2.12/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.kamon/kamon-cassandra_2.12)


The <b>kamon-cassandra</b> module requires you to start your application using the Kanela Agent. Kamon will warn you
at startup if you failed to do so.

The bytecode instrumentation provided by the `kamon-cassandra-client` module hooks into the Cassandra Driver to automatically
gather Metrics and start and finish Spans for requests that are issued within a trace. This translates into you having metrics about how
the requests you are doing are behaving.

### Getting Started

Kamon Cassandra module is currently available for Scala 2.11 and 2.12.

Supported releases and dependencies are shown below.

| kamon-cassandra  | status | jdk  | scala            
|:------:|:------:|:----:|------------------
|  1.0.6 | stable | 1.8+ | 2.11, 2.12  

To get started with SBT, simply add the following to your `build.sbt`
file:

```scala
libraryDependencies += "io.kamon" %% "kamon-cassandra-client" % "1.0.6"
```

### Metrics ###

The following metrics will be recorded:

- cassandra_query_count_total
- cassandra_client_inflight_driver_bucket
- cassandra_trashed_connections_bucket
- cassandra_connection_pool_size_bucket
- cassandra_query_duration_seconds_bucket
- cassandra_client_inflight_bucket
