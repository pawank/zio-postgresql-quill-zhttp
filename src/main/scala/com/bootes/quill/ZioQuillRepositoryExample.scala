package com.bootes.quill

import com.bootes.quill.repository.UserRepository
import zio._
import zio.console._
import zio.magic._

object ZioQuillRepositoryExample extends App {

  val items = Seq(CreateUserRequest.sample)

  val startup: ZIO[Has[UserService], Throwable, Seq[User]] = ZIO.foreachPar(items)(UserService.create)

  val program: ZIO[Console with Has[UserService], Throwable, Seq[User]] =
    (startup *> UserService.all.tap(a => putStrLn(s"Found: \n\t${a.mkString("\n\t")}")))

  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] =
    program.inject(Console.live, ZioQuillContext.dataSourceLayer, UserService.layer, UserRepository.layer).exitCode
}
