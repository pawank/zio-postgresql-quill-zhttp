package com.bootes.quill.repository

import com.bootes.quill.{CreateUserRequest, User}
import io.getquill.context.ZioJdbc.QDataSource
import zio._
import zio.macros.accessible

@accessible
trait UserRepository {
  def create(item: User): Task[User]
  def all: Task[Seq[User]]
  def findById(id: Long): Task[User]
}

object UserRepository {
  val layer: URLayer[QDataSource, Has[UserRepository]] = (UserRepositoryLive(_, _)).toLayer
}
