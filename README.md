# R2DBC First Example in Scala

[![Build Status](https://travis-ci.org/seratch/r2dbc-examples-scala.svg?branch=master)](https://travis-ci.org/seratch/r2dbc-examples-scala)

This repository demonstrates how to use [R2DBC - Reactive Relational Database Connectivity](http://r2dbc.io/) in Scala.

As of Dec 2018, R2DBC is still under the development. Thus, there are still many limitations:

* Supported RDBMS: PostgreSQL, H2, and MS SQL Server
* CLOB/BLOB unsupported
* No connection pooling

In this example, I use [`r2dbc-client`](https://github.com/r2dbc/r2dbc-client) which is the reference implementation of [`r2dbc-spi`](https://github.com/r2dbc/r2dbc-spi), the service provider interface. `r2dbc-client` heavily depends on [Reactor](https://projectreactor.io/), whereas the R2DBC SPI doesn't have any relatiojns with Reactor. The SPI relies on only [Reactive Streams](http://www.reactive-streams.org/). Thanks to the  thin SPI interface, it should be much easier to implement your own R2DBC clients than toilsome JDBC drivers.

While working with `r2dbc-client`, you will see lots of [`Flux`](https://projectreactor.io/docs/core/release/api/reactor/core/publisher/Flux.html)/[`Mono`](https://projectreactor.io/docs/core/release/api/reactor/core/publisher/Mono.html) objects. As far as I know, Reactor is not so popular in the Scala world (The Scala community already has many other options to achieve the same goal). You may feel like "What's `Flux`/`Mono`...? Do I need to learn ways to use it?" Don't worry, be happy. You don't need to learn the details of it. They are just enriched `Publisher` objects in the context of Reactive Streams. So, if you're familiar with the `Publisher`, you can simply handle `Flux`/`Mono` as `Publisher`. Otherwise, I recommend you to transform those types to your favorites such as [`Observable`](https://monix.io/docs/3x/reactive/observable.html) in [monix-reactive](https://monix.io/docs/3x/#monix-reactive), [`Stream` in fs2](https://fs2.io/guide.html), etc.

# Getting Started

## build.sbt

`r2dbc-client` is always mandatory. In addition to that, you must have the necessary dialect, any of `r2dbc-h2`, `r2dbc-postgresql`, and `r2dbc-mssql`. Don't forget to have the `spring-milestone` resolver. R2DBC libraries are not available on the Maven Central yet.

```scala
// Minimum settings for R2DBC
lazy val root = (project in file("."))
  .settings(
    scalaVersion := "2.12.8",
    resolvers += "spring-milestone" at "https://repo.spring.io/milestone",
    libraryDependencies ++= Seq(
      "io.r2dbc" % "r2dbc-client"     % "1.0.0.M6"  % Compile,
      "io.r2dbc" % "r2dbc-h2"         % "1.0.0.M6"  % Compile,
      "io.r2dbc" % "r2dbc-postgresql" % "1.0.0.M6"  % Compile
    )
  )

// You can build Monix's Observable from Flux/Mono instantly
libraryDependencies += "io.monix" %% "monix-reactive" % "3.0.0-RC2" % Compile

// If you need the ways to convert Flux/Mono to Scala Future
libraryDependencies += "org.scala-lang.modules" %% "scala-java8-compat" % "0.9.0" % Compile
```

## Code Example

Here is a simple code example to run queries on H2 in-memory database. Apart from the part to build `io.r2dbc.client.R2dbc`, the exactly same code works for PostgreSQL. 

To make the example simple, the code intentionally does blocking operations. Needless to say, you must NOT do the same in real application code but following the best practices to leverage the benefits of reactive / non-blocking model.

```scala
val config = io.r2dbc.h2.H2ConnectionConfiguration.builder().url("mem:sample1").build()
val r2dbc  = new io.r2dbc.client.R2dbc(new io.r2dbc.h2.H2ConnectionFactory(config))

// Create the table beforehand
r2dbc
  .withHandle(_.execute("create table sample (id bigint primary key, name varchar(100))"))
  .blockFirst() // Flux#blockFirst()

val result: reactor.core.publisher.Flux[Sample] = {
  // Insert three rows
  val insertions: reactor.core.publisher.Flux[Integer] = r2dbc.inTransaction { handle =>
    val updates = handle.createUpdate("insert into sample (id, name) values ($1, $2)")
    updates.bind("$1", 1).bind("$2", "Alice").add()
    updates.bind("$1", 2).bind("$2", "Bob").add()
    updates.bind("$1", 3).bindNull("$2", classOf[String]).add()
    updates.execute()
  }
  // Select all the rows descending order
  val fetchingAll: reactor.core.publisher.Flux[Sample] = r2dbc.withHandle { handle =>
    handle.select("select id, name from sample order by id desc").mapRow { row =>
      Sample(
        id = Long.unbox(row.get("id", classOf[java.lang.Long])),
        name = Option(row.get("name", classOf[String]))
      )
    }
  }
  // 'fetchingAll' will be executed after the completion of 'insertions'
  insertions.thenMany(fetchingAll)
}
```

To work with `Publisher` more easily, I recommend you to use `Observable` from `monix-reactive`. Refer to [the Monix document](https://monix.io/docs/3x/reactive/observable.html) for details.

```scala
// simple example to run with monix-reactive
import monix.reactive.Observable
val observable: Observable[Sample] = Observable.fromReactivePublisher(result)

// Just for demonstration, intentionally does the blocking ops here
import monix.execution.Scheduler.Implicits.global
val samples: Seq[Sample] = observable.toListL.runSyncUnsafe()
samples.map(_.id) should equal(Seq(3, 2, 1))
```

# References

* [Reactive Streams](http://www.reactive-streams.org/)
* [R2DBC Website](http://r2dbc.io/)
* [R2DBC GitHub Organization](https://github.com/r2dbc)
* ["Reactive Relational Database Connectivity" at SpringOne Platform 2018](https://www.youtube.com/watch?v=idApf9DMdfk)
* [Project Reactor](https://projectreactor.io/)
* [Reactor Flux javadoc](https://projectreactor.io/docs/core/release/api/reactor/core/publisher/Flux.html)
* [Monix Website](https://monix.io/)
* [Monix Reactive - Observable](https://monix.io/docs/3x/reactive/observable.html)
* [Monix Eval - Task](https://monix.io/docs/3x/eval/task.html)
* [Monix Reactive](https://monix.io/docs/3x/#monix-reactive)