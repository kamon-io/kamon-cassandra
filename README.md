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
libraryDependencies += "io.kamon" %% "kamon-cassandra-client" % "2.0.0"
```


##Connection metrics

`TargetTags` include (depending on the configuration) 
    - `cassandra.target` execution target
    - `cassandra.dc` target datacenter
    - `cassandra.cluster` target cluster
 
####`cassandra.client.pool.borrow-time`
- Time spent waiting for a connection for an execution
- Timer
    - `TargetTags`

####`cassandra.connection.pool.size`
- Number of active connections per node
- Histogram
    - `TargetTags`

####`cassandra.client.pool.trashed`
- Number of thrashed connections per host
- Counter
    - `TargetTags`


####`cassandra.client.pool.connection.in-flight`
- Distribution of in-flight requests on this connection measured at the moment a new query is issued
- Histogram


####`cassandra.client.pool.target.in-flight`
- Distribution of of in-flight request towards over hosts measured at the moment a new query is issued
- Histogram
    - `TargetTags`


##Query metrics


####`span.processing-time` - `operation=cassandra.client.query` (not including retries, speculations, fetches..)
- Histogram
- User observed query duration, as measured from the moment query is `executeAsync` is invoked until first page of result set is ready
    - `cassandra.query.kind` -> present only for DML statements `select` | `insert` | `update` | `delete`

####`span.processing-time` - `operation=cassandra.client.query.${execution-type}` 
- Available execution types:
    - prepare
    - execution
    - speculative
- Histogram
- Client observed target-execution duration
    - `retry` - indicates execution is retry of a previously failed execution


####`cassandra.client.inflight`
- Current number of of active queries
- RangeSampler 
    - `target` -> Not used? should be host, but dont have that info on session, maybe doesnt make sense for query (does for execution?)

####`cassandra.query.errors`
- Counter 
- Count total number of failed executions (not neccessarily failed entire query)
    - `TargetTags`

####`cassandra.query.timeouts`
- Counter 
- Count total number of timed-out executions
    - `TargetTags`

####`cassandra.query.retries`
- Counter 
- Count cluster-wide total number of retried exectutions
    - `TargetTags`

####`cassandra.query.speculative`
- Counter 
- Count cluster-wide total number of speculative executions (only issued queries, not measuring whether speculative won or got canceled by original response arriving)
    - `TargetTags`

####`cassandra.query.cancelled`
- Counter 
- Count cluster-wide total number of cancelled executions (including user hanging up or speculative execution getting cancelled)
    - `TargetTags`




##Executor metrics:

####`cassandra.queue.executor`
- Gauge 
- Number of queued up tasks in the main internal executor.
####`cassandra.queue.blocking`
- Gauge 
- Number of queued up tasks in the blocking executor.
####`cassandra.queue.reconnection`
- Gauge 
- Number of queued up tasks in the reconnection executor
####`cassandra.scheduled-tasks`
- Gauge 
- Number of queued up tasks in the scheduled tasks executor.




##Tracing
Client Span is created for every `executeAsync` invocation tagged with
- `span.kind` - `client`
- `cassandra.query` Query CQL
- `cassandra.keyspace`
- `cassandra.query.kind` - DML statement kind
- `cassandra.client.rs.session-id` Correlation id for server tracing (if enabled)
- `cassandra.client.rs.cl` - Achieved consistency level
- `cassandra.client.rs.fetch-size` - Requested fetch size
- `cassandra.client.rs.fetched` - Rows retrieved in by current executuion
- `cassandra.client.rs.has-more` - Indicates resultset is not fully exhausted yet


As a child of each client span, one span is created per execution. These execution span can represent
initial or subsequent fetches (triggered by iterating through result set past first page), internal retries
based on retry policy or speculative executions.


Operation name indicates the type of execution
    `cassandra.client.query.prepare|execution|speculative`

Execution spans are tagged with actual target information:
- `dc`
- `rack`
- `target`



Retried executions are additionally tagged with 
- `retry` -> true.


Execution spon start time indicates start of execution which also includes connection pooling time,
mark `writing` is used to indicate moment connection is aquired an request gets written to the wire.

