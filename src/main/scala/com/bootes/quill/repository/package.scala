package com.bootes.quill

import io.getquill.context.jdbc.JdbcRunContext
import io.getquill.{NamingStrategy, PostgresZioJdbcContext, SnakeCase}
import zio.json.{DeriveJsonCodec, JsonCodec}

import java.sql.{Timestamp, Types}
import java.time.{Instant, ZoneId, ZonedDateTime}

package object repository {
  import java.nio.charset.Charset

  object JSONB {
    def apply(string: String, charset: Charset = Charset.defaultCharset): JSONB = new JSONB(string.getBytes(charset))

    def stringify(jsonb: JSONB, charset: Charset = Charset.defaultCharset): String = new String(jsonb.bytes, charset)
  }

  case class JSONB(bytes: Array[Byte]) extends AnyVal

  case class InvoiceRecord(id: Long = -1, userId: Long, total: Double, paid: Boolean, createdAt: Instant)

  class MyZioContext[N <: NamingStrategy](override val naming: N) extends PostgresZioJdbcContext[N](naming) with InstantEncoding

  object MyContext extends PostgresZioJdbcContext(SnakeCase) with InstantEncoding

  //noinspection DuplicatedCode
  trait InstantEncoding { this: JdbcRunContext[_, _] =>
    implicit val instantDecoder: Decoder[Instant] = decoder((index, row) => { row.getTimestamp(index).toInstant })
    implicit val instantEncoder: Encoder[Instant] = encoder(Types.TIMESTAMP, (idx, value, row) => row.setTimestamp(idx, Timestamp.from(value)))
    implicit val jsonbDecoder: Decoder[JSONB] = decoder((index, row) => { JSONB(row.getBytes(index)) })
    implicit val jsonbEncoder: Encoder[JSONB] = encoder(Types.BINARY, (idx, value, row) => row.setBytes(idx, value.bytes))
    implicit val zoneDateTimeEncoder: Encoder[ZonedDateTime] = encoder(Types.TIMESTAMP_WITH_TIMEZONE, (index, value, row) => row.setTimestamp(index, Timestamp.from(value.toInstant)))
    implicit val zoneDateTimeDecoder: Decoder[ZonedDateTime] = decoder((index, row) => { ZonedDateTime.ofInstant(row.getTimestamp(index).toInstant, ZoneId.of("UTC")) })
  }



  case class NotFoundException(message: String, id: Long) extends Throwable
}
