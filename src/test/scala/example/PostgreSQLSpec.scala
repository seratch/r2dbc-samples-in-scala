package example

import org.scalatest._

class PostgreSQLSpec extends FlatSpec with Matchers {

  case class Sample(id: Long, name: Option[String])

  "r2dbc" should "work with PostgreSQL" in {

    import io.r2dbc.client.R2dbc
    import io.r2dbc.postgresql.{ PostgresqlConnectionConfiguration, PostgresqlConnectionFactory }

    val config = PostgresqlConnectionConfiguration
      .builder()
      .host("localhost")
      .database("r2dbc")
      .username("sa")
      .password("sa")
      .build()

    val r2dbc = new R2dbc(new PostgresqlConnectionFactory(config))

    import reactor.core.publisher.Flux

    try r2dbc.withHandle(_.execute("drop table sample")).blockFirst()
    catch { case scala.util.control.NonFatal(e) => e.printStackTrace() }

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
    import monix.reactive.Observable
    val observable: Observable[Sample] = Observable.fromReactivePublisher(result)

    import monix.execution.Scheduler.Implicits.global
    val samples = observable.toListL.runSyncUnsafe()
    samples.size should equal(3)
    samples.map(_.id) should equal(Seq(3, 2, 1))
  }

}
