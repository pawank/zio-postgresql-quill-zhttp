package com.bootes.quill

import com.bootes.quill.repository.{JSONB, UserRepository}
import io.getquill.Embedded
import io.scalaland.chimney.dsl.TransformerOps
import zio.{Has, RIO, RLayer, Task, ZIO}
import zio.console.Console
import zio.json.{DeriveJsonCodec, JsonCodec}
import zio.macros.accessible

import java.time.{Instant, LocalDateTime, ZoneId, ZonedDateTime}
import scala.language.implicitConversions

case class PrimaryAddress(houseNo: String, street: Option[String] = None, pincode: Option[String] = None,
                   landmark: Option[String] = None, city: Option[String] = None,
                   state: Option[String] = None,
                   country: String = "IN",
                   fullAddress: Option[String] = None) extends Embedded

object PrimaryAddress {
  implicit val codec: JsonCodec[PrimaryAddress] = DeriveJsonCodec.gen[PrimaryAddress]
}

case class Address(id: Long = -1, `type`: String, houseNo: String, street: Option[String] = None, pincode: Option[String] = None,
                   landmark: Option[String] = None, city: Option[String] = None,
                   state: Option[String] = None,
                   country: String = "IN",
                   fullAddress: Option[String] = None,
                   metadata: Option[Metadata] = Some(Metadata.default),
                   userId: Long)

object Address {
  implicit val codec: JsonCodec[Address] = DeriveJsonCodec.gen[Address]
}

case class Metadata(createdAt: java.time.ZonedDateTime = ZonedDateTime.now(ZoneId.of("UTC")),
                    updatedAt: Option[ZonedDateTime],
                    createdBy: String = "system",
                    updatedBy: Option[String]) extends Embedded
object Metadata {
  implicit val codec: JsonCodec[Metadata] = DeriveJsonCodec.gen[Metadata]
  val default = Metadata(updatedAt = None, updatedBy = None)
}

case class VectorInfo(pancard: Option[String],
                      passportNo: Option[String]) extends Embedded
object VectorInfo {
  implicit val codec: JsonCodec[VectorInfo] = DeriveJsonCodec.gen[VectorInfo]
}


case class LegalEntity(registrationNo: String,
                       legalName: String,
                       `type`: String,
                       mailingAddress: Option[Address],
                       registeredAddress: Option[Address],
                       status: String) extends Embedded
object LegalEntity {
  implicit val codec: JsonCodec[LegalEntity] = DeriveJsonCodec.gen[LegalEntity]
}


case class PiiInfo(firstName: String,
                   middleName: Option[String] = None,
                   lastName: String,
                   email1: Option[String] = None,
                   email2: Option[String] = None,
                   email3: Option[String] = None,
                   phone1: Option[String] = None,
                   phone2: Option[String] = None,
                   phone3: Option[String] = None) extends Embedded
object PiiInfo {
  implicit val codec: JsonCodec[PiiInfo] = DeriveJsonCodec.gen[PiiInfo]
}

case class Attribute(key: String, value: String)
object Attribute {
  implicit val codec: JsonCodec[Attribute] = DeriveJsonCodec.gen[Attribute]
}


case class User(
  id: Long = -1,
  `type`: String,
  code: String,
  pii: PiiInfo,
  vector: Option[VectorInfo] = None,
  gender: Option[String] = None,
  dateOfBirth: Option[java.time.LocalDate] = None,
  placeOfBirth: Option[String] = None,
  address: Option[PrimaryAddress] = None,
  //legalEntity: LegalEntity,
  roles: List[String] = List.empty,
  scopes: List[String] = List.empty,
  status: String = "active",
  metadata: Option[Metadata] = None
  )

case class CreateUserRequest (
                 `type`: String,
                 code: String,
                 pii: PiiInfo,
                 vector: Option[VectorInfo] = None,
                 gender: Option[String] = None,
                 dateOfBirth: Option[java.time.LocalDate] = None,
                 placeOfBirth: Option[String] = None,
                 address: Option[Address] = None,
                 //legalEntity: LegalEntity,
                 //attributes: Map[String, String] = Map.empty,
                 //attributes: JSONB = JSONB.apply(String.valueOf("{}").getBytes()),
                 roles: List[String] = List.empty,
                 scopes: List[String] = List.empty,
                 status: String = "active"
               )
object CreateUserRequest {
  implicit val codec: JsonCodec[CreateUserRequest] = DeriveJsonCodec.gen[CreateUserRequest]
  val sample = CreateUserRequest(`type` = "real", code = "111", pii = PiiInfo(firstName = "Pawan", lastName = "Kumar"), status = "active")
}

object User {
  implicit def fromUserRecord(record: CreateUserRequest): User               = record.into[User].transform.copy(metadata = Some(Metadata.default))
  implicit def fromSeqUserRecord(records: Seq[CreateUserRequest]): Seq[User] = records.map(fromUserRecord)
  implicit val codec: JsonCodec[User]                                 = DeriveJsonCodec.gen[User]

  val sample = User(`type` = "real", code = "1", pii = PiiInfo(firstName = "Pawan", lastName = "Kumar"), status = "active", metadata = Some(Metadata.default))
}

@accessible
trait UserService {
  def create(request: CreateUserRequest): Task[User]
  def all: Task[Seq[User]]
  def get(id: Long): Task[User]
}

object UserService {
  val layer: RLayer[Has[UserRepository] with Console, Has[UserService]] = (UserServiceLive(_, _)).toLayer
}

case class UserServiceLive(repository: UserRepository, console: Console.Service) extends UserService {

  override def create(request: CreateUserRequest): Task[User] = {
    repository.create(User.fromUserRecord(request))
  }

  override def all: Task[Seq[User]] = for {
    users <- repository.all
    _     <- console.putStrLn(s"Users: ${users.map(_.code).mkString(",")}")
  } yield users.sortBy(_.id)

  override def get(id: Long): Task[User] = repository.findById(id)
}
