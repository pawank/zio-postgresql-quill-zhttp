package com.bootes.zhttp

import com.bootes.quill.{UserService, ZioQuillContext}
import com.bootes.quill.{UserService, ZioQuillContext}
import com.bootes.quill.repository.{UserRepository, NotFoundException}
import com.bootes.zhttp.auth.AuthenticationApp
import zhttp.http._
import zhttp.service.Server
import zio._
import zio.console._
import zio.magic._

object UserServer extends App {

  val endpoints: Http[Has[UserService] with Console, HttpError, Request, Response[Has[UserService] with Console, HttpError]] =
    AuthenticationApp.login +++ CORS(
      AuthenticationApp.authenticate(HttpApp.forbidden("None shall pass."), UserEndpoints.user),
      config = CORSConfig(anyOrigin = true)
    ) +++ InvoiceEndpoints.invoiceRoutes

  val program: ZIO[Any, Throwable, Nothing] = Server
    .start(8080, endpoints)
    .inject(Console.live, ZioQuillContext.dataSourceLayer, UserService.layer, UserRepository.layer)

  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] = program.exitCode
}
