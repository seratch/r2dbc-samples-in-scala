package example

import org.scalatest._

class H2Spec extends FlatSpec with Matchers {

  case class Sample(id: Long, name: Option[String])

  "r2dbc + monix" should "work with H2 database" in {

    import io.r2dbc.client.R2dbc
    import io.r2dbc.h2.{ H2ConnectionConfiguration, H2ConnectionFactory }

    val config = H2ConnectionConfiguration.builder().url("mem:sample2").build()
    val r2dbc  = new R2dbc(new H2ConnectionFactory(config))

    import reactor.core.publisher.Flux

    r2dbc
      .withHandle(_.execute("drop table sample if exists"))
      .blockFirst()
    r2dbc
      .withHandle(_.execute("create table sample (id bigint primary key, name varchar(100))"))
      .blockFirst()

    val result: Flux[Sample] = {
      val insertions: Flux[Integer] = r2dbc.inTransaction { handle =>
        // Identifier '$id' is not a valid identifier. Should be of the pattern '.*\$([\d]+).*'
        val updates = handle.createUpdate("insert into sample (id, name) values ($1, $2)")
        updates.bind("$1", 1).bind("$2", "Alice").add()
        updates.bind("$1", 2).bind("$2", "Bob").add()
        updates.bind("$1", 3).bindNull("$2", classOf[String]).add()
        updates.execute()
      }
      val fetchingAll: Flux[Sample] = r2dbc.withHandle { handle =>
        handle.select("select id, name from sample order by id desc").mapRow { row =>
          Sample(id = Long.unbox(row.get("id", classOf[java.lang.Long])),
                 name = Option(row.get("name", classOf[String])))
        }
      }
      insertions.thenMany(fetchingAll)
    }

    // simple example to run with monix-reactive
    import monix.reactive.Observable
    val observable: Observable[Sample] = Observable.fromReactivePublisher(result)

    import monix.execution.Scheduler.Implicits.global
    val samples = observable.toListL.runSyncUnsafe()
    samples.size should equal(3)
    samples.map(_.id) should equal(Seq(3, 2, 1))
  }

  "r2dbc" should "work with H2 database" in {

    import io.r2dbc.client.R2dbc
    import io.r2dbc.h2.{ H2ConnectionConfiguration, H2ConnectionFactory }

    val config = H2ConnectionConfiguration.builder().url("mem:sample1").build()
    val r2dbc  = new R2dbc(new H2ConnectionFactory(config))

    import reactor.core.publisher.Flux

    r2dbc
      .withHandle(_.execute("drop table sample1 if exists"))
      .blockFirst()
    r2dbc
      .withHandle(_.execute("create table sample (id bigint primary key, name varchar(100))"))
      .blockFirst()

    val result: Flux[Sample] = {
      val insertions: Flux[Integer] = r2dbc.inTransaction { handle =>
        // Identifier '$id' is not a valid identifier. Should be of the pattern '.*\$([\d]+).*'
        val updates = handle.createUpdate("insert into sample (id, name) values ($1, $2)")
        updates.bind("$1", 1).bind("$2", "Alice").add()
        updates.bind("$1", 2).bind("$2", "Bob").add()
        updates.bind("$1", 3).bindNull("$2", classOf[String]).add()
        updates.execute()
      }
      val fetchingAll: Flux[Sample] = r2dbc.withHandle { handle =>
        handle.select("select id, name from sample order by id desc").mapRow { row =>
          Sample(
            id = Long.unbox(row.get("id", classOf[java.lang.Long])),
            name = Option(row.get("name", classOf[String]))
          )
        }
      }
      insertions.thenMany(fetchingAll)
    }

    // simple example to run with monix-reactive
    import java.util.concurrent.CompletableFuture
    val jFuture: CompletableFuture[java.util.List[Sample]] = result.collectList().toFuture
    import scala.collection.JavaConverters._
    import scala.compat.java8.FutureConverters._
    import scala.concurrent.ExecutionContext.Implicits.global
    import scala.concurrent.duration.Duration
    import scala.concurrent.{ Await, Future }
    val f: Future[Seq[Sample]] = jFuture.toScala.map(_.asScala)
    val allRows: Seq[Sample]   = Await.result(f, Duration.Inf)
    allRows.size should equal(3)
    allRows.map(_.id) should equal(Seq(3, 2, 1))
  }

}
