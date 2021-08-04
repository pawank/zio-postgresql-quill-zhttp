package com.bootes.zhttp

import com.bootes.quill.User
import com.bootes.zhttp.auth.LoginRequest
import zhttp.core.ByteBuf
import zhttp.http.HttpData.CompleteData
import zhttp.http._
import zhttp.service.{ChannelFactory, Client, EventLoopGroup}
import zio._
import zio.console._
import zio.json._

object ClientExample extends App {

  val env: TaskLayer[ChannelFactory with EventLoopGroup] = ChannelFactory.auto ++ EventLoopGroup.auto()
  val loginUrl: String                                   = "http://localhost:8080/login"
  val itemsUrl: String                                   = "http://localhost:8080/items"
  val token: String                                      = ""
  val headers                                            = List(Header.authorization(s"Bearer $token"))
  val loginRequest: LoginRequest                         = LoginRequest("Foobar", "rabooF")

  val login: ZIO[EventLoopGroup with ChannelFactory, Throwable, String] =
    for {
      login        <- ZIO.fromEither(URL.fromString(loginUrl))
      loginByteBuf <- ByteBuf.fromString(loginRequest.toJson)
      loginRequest  = Request(endpoint = (Method.POST -> login), headers = List.empty, content = HttpData.fromByteBuf(loginByteBuf.asJava))
      res          <- Client.request(loginRequest)
      token         = res.content match {
        case CompleteData(data) => data.map(_.toChar).mkString
        case _                  => "<you shouldn't see this>"
      }
    } yield token

  def getUsers(token: String): ZIO[EventLoopGroup with ChannelFactory, Serializable, Seq[User]] =
    for {
      item  <- ZIO.fromEither(URL.fromString(itemsUrl))
      res   <- Client.request(Request(endpoint = Method.GET -> item, headers = List(Header.authorization(s"Bearer $token"))))
      items <- ZIO.fromEither(res.content match {
        case CompleteData(data) => data.map(_.toChar).mkString.fromJson[Seq[User]]
        case _                  => Left("Unexpected data type")
      })
    } yield items

  val program: ZIO[Console with EventLoopGroup with ChannelFactory, Serializable, Unit] =
    for {
      items         <- login >>= getUsers
      namesAndPrices = items.map(i => i.code -> i.status)
      _             <- putStrLn(s"Found items:\n\t${namesAndPrices.mkString("\n\t")}")
    } yield ()

  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] = program.provideLayer(ZEnv.live ++ env).exitCode
}
