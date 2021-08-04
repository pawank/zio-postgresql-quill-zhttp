package com.bootes.zhttp

import com.bootes.quill.{CreateUserRequest, UserService}
import com.bootes.quill.repository.NotFoundException
import com.bootes.quill.{CreateUserRequest, UserService}
import pdi.jwt.JwtClaim
import zhttp.http._
import zio.console._
import zio.json._
import zio.{Has, IO, ZIO}

object UserEndpoints extends RequestOps {

  val user: JwtClaim => Http[Has[UserService] with Console, HttpError, Request, UResponse] = jwtClaim =>
    Http
      .collectM[Request] {
        case Method.GET -> Root / "users"        =>
          for {
            _     <- putStrLn(s"Validated claim: $jwtClaim")
            users <- UserService.all
          } yield Response.jsonString(users.toJson)
        case Method.GET -> Root / "users" / id   =>
          for {
            _    <- putStrLn(s"Validated claim: $jwtClaim")
            user <- UserService.get(id.toInt)
          } yield Response.jsonString(user.toJson)
        case req @ Method.POST -> Root / "users" =>
          for {
            _       <- putStrLn(s"Validated claim: $jwtClaim")
            request <- extractBodyFromJson[CreateUserRequest](req)
            results <- UserService.create(request)
          } yield Response.jsonString(results.toJson)
      }
      .catchAll {
        case NotFoundException(msg, id) =>
          Http.fail(HttpError.NotFound(Root / "users" / id.toString))
        case ex: Throwable              =>
          Http.fail(HttpError.InternalServerError(msg = ex.getMessage, cause = Option(ex)))
        case err                        => Http.fail(HttpError.InternalServerError(msg = err.toString))
      }
}

trait RequestOps {

  def extractBodyFromJson[A](request: Request)(implicit codec: JsonCodec[A]): IO[Serializable, A] =
    for {
      requestOrError <- ZIO.fromOption(request.getBodyAsString.map(_.fromJson[A]))
      body           <- ZIO.fromEither(requestOrError)
    } yield body
}
