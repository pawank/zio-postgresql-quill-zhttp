package com.bootes.quill.repository

import com.bootes.quill.User
import io.getquill.context.ZioJdbc.QuillZioExt
import zio._
import zio.blocking.Blocking

import java.io.Closeable
import javax.sql.DataSource

case class UserRepositoryLive(dataSource: DataSource with Closeable, blocking: Blocking.Service) extends UserRepository {
  val dataSourceLayer: Has[DataSource with Closeable] with Has[Blocking.Service] = Has.allOf[DataSource with Closeable, Blocking.Service](dataSource, blocking)

  import MyContext._

  override def create(user: User): Task[User] = transaction {
    for {
      _     <- run(UserQueries.insertUser(user))
      users <- run(UserQueries.usersQuery)
    } yield users.headOption.getOrElse(throw new Exception("Insert failed!"))
  }.dependOnDataSource().provide(dataSourceLayer)

  override def all: Task[Seq[User]] = run(UserQueries.usersQuery).dependOnDataSource().provide(dataSourceLayer)

  override def findById(id: Long): Task[User] = {
    for {
      results <- run(UserQueries.byId(id)).dependOnDataSource().provide(dataSourceLayer)
      user    <- ZIO.fromOption(results.headOption).orElseFail(NotFoundException(s"Could not find user with id $id", id))
    } yield user
  }

}

object UserQueries {

  import MyContext._

  // NOTE - if you put the type here you get a 'dynamic query' - which will never wind up working...
  implicit val userSchemaMeta = schemaMeta[User](""""user"""")
  implicit val userInsertMeta = insertMeta[User](_.id)

  val usersQuery                   = quote(query[User])
  def byId(id: Long)               = quote(usersQuery.filter(_.id == lift(id)))
  def insertUser(user: User) = quote(usersQuery.insert(lift(user)))
}
