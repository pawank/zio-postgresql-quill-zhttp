package com.bootes.quill

import com.bootes.quill.repository.JSONB
import io.getquill.context.ZioJdbc.{QConnection, QDataSource}
import io.getquill.{PostgresZioJdbcContext, SnakeCase}
import zio._
import zio.blocking.Blocking
import zio.console._

import java.sql.{Timestamp, Types}
import java.time.{Instant, LocalDate, LocalDateTime, ZoneId, ZonedDateTime}
import java.util.{Date, UUID}

object ZioQuillExample extends App {
  // new ZIO JDBC Context for Quill!
  val ctx = new PostgresZioJdbcContext(SnakeCase)
  import ctx._

  // some Meta classes to help Quill
  implicit val userSchemaMeta = schemaMeta[User](""""user"""")
  implicit val userInsertMeta = insertMeta[User](_.id)

  // some Encoders for Instant so Quill knows what to do with an Instant
  implicit val instantEncoder: Encoder[Instant] = encoder(Types.TIMESTAMP, (index, value, row) => row.setTimestamp(index, Timestamp.from(value)))
  implicit val instantDecoder: Decoder[Instant] = decoder((index, row) => { row.getTimestamp(index).toInstant })
  implicit val jsonbDecoder: Decoder[JSONB] = decoder((index, row) => { JSONB(row.getBytes(index)) })
  implicit val jsonbEncoder: Encoder[JSONB] = encoder(Types.BINARY, (idx, value, row) => row.setBytes(idx, value.bytes))
  implicit val zoneDateTimeEncoder: Encoder[ZonedDateTime] = encoder(Types.TIMESTAMP_WITH_TIMEZONE, (index, value, row) => row.setTimestamp(index, Timestamp.from(value.toInstant)))
  implicit val zoneDateTimeDecoder: Decoder[ZonedDateTime] = decoder((index, row) => { ZonedDateTime.ofInstant(row.getTimestamp(index).toInstant, ZoneId.of("UTC")) })

  // simple layer providing a connection for the effect; this is pulled from a HikariCP
  // NOTE - prefix is the HOCON prefix in the application.conf to look for
  val zioConn: ZLayer[Blocking, Throwable, QConnection] =
    QDataSource.fromPrefix("zioQuillExample") >>> QDataSource.toConnection

  // an user to insert...
  val anItem: User = User.sample

  // some Quill queries
  val usersQuery                = quote(query[User])
  def insertItem(user: User) = quote(usersQuery.insert(lift(anItem)))

  // the transactional use of the context (this belongs in a DAO/Repository ZIO Service module)
  val insertAndQuery: RIO[QConnection, List[User]] = ctx.transaction {
    for {
      _     <- {
        val s = ctx.translate(insertItem(anItem))
        ZIO.effect(println(s)) *> ctx.run(insertItem(anItem))
      }
      users <- ctx.run(usersQuery)
    } yield users
  }

  // our program!
  val program: RIO[Console with QConnection, Unit] = for {
    _     <- putStrLn("Running zio-quill example...")
    users <- insertAndQuery
    _     <- putStrLn(s"Items ==> $users")
  } yield ()

  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] = program.provideLayer(ZEnv.live ++ zioConn).exitCode
}