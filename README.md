Cassandra Integration   ![Build Status](https://travis-ci.org/kamon-io/kamon-cassandra.svg?branch=master)
==========================

[![Gitter](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/kamon-io/Kamon?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.kamon/kamon-cassandra_2.12/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.kamon/kamon-cassandra_2.12)


The `kamon-cassandra` module brings bytecode instrumentation to trace jdbc-compatible database requests

The <b>kamon-cassandra</b> module requires you to start your application using the Kanela Agent. Kamon will warn you
at startup if you failed to do so.

The bytecode instrumentation provided by the `kamon-cassandra` module hooks into the Cassandra Driver to automatically
start and finish Spans for requests that are issued within a trace. This translates into you having metrics about how
the requests you are doing are behaving.

### Getting Started

Kamon Cassandra module is currently available for Scala 2.11 and 2.12.

Supported releases and dependencies are shown below.

| kamon-cassandra  | status | jdk  | scala            
|:------:|:------:|:----:|------------------
|  1.0.0 | stable | 1.8+ | 2.11, 2.12  

To get started with SBT, simply add the following to your `build.sbt`
file:

```scala
libraryDependencies += "io.kamon" %% "kamon-cassandra" % "1.0.0"
```


### Metrics ###

The following metrics will be recorded:

* __reads__: a histogram that tracks the reads requests latency (SELECT statement).
* __writes__: a histogram that tracks the writes requests latency (INSERT, UPDATE, and DELETE statements).
* __slows__: a simple counter with the number of measured slow requests.
* __errors__: a simple counter with the number of failures.

