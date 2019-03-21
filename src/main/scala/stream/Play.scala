package stream

import akka.NotUsed
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.Source
import cats._
import cats.data._
import cats.implicits._
import stream.Play.{CommittableElement, CommittableOffset, Incoming}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.util.Try

object Play {
  case class CommittableOffset(offset: Long)

  type CommittableElement[A] = (CommittableOffset, A)

  val c: Functor[CommittableElement] = Functor[CommittableElement]
  val t: Traverse[CommittableElement] = Traverse[CommittableElement]

  case class Incoming(hexString: String)
  case class Parsed(num: Int)
  case class Cubed(num: Int)
  case class Result(isEven: Boolean)

  sealed trait Errors
  case class ParseError(input: Incoming) extends Errors
  case class NotInWhitelist() extends Errors

  def parse(in: Incoming): Either[Errors, Parsed] = {
    Try(in.hexString.toInt).fold(
      _ => Left(ParseError(in)),
      int => Right(Parsed(int))
    )
  }
  def cube(p: Parsed): Cubed = Cubed(p.num ^ 3)
  def whiteListAsync(cubed: Cubed): Future[Either[NotInWhitelist, Cubed]] = {
    if (cubed.num < 1000) Future.successful(Either.right(cubed))
    else Future.successful(Either.left(NotInWhitelist()))
  }
  def isEven(p: Cubed): Result = if (p.num % 2 == 0) Result(true) else Result(false)

  import syntax.TupleExt._
  ("", 5).left

  val src: Source[CommittableElement[Either[Errors, Result]], NotUsed] = {
    KafkaSource()
      .map(c.map(_)(parse))
      .buffer(1000, OverflowStrategy.backpressure)
      .map(EitherT.apply)
      .map(_.map(cube))
      .mapAsync(5)(e => e.traverse(whiteListAsync))
      .map(_.subflatMap(identity))
      .throttle(1, 1.second)
      .map(_.map(isEven))
      .map(_.value)
  }

  // To get .asStreamT
  import stream.syntax.SourceExt._

  val src2: Source[CommittableElement[Either[Errors, Result]], NotUsed] = {
    StreamT(KafkaSource())
      .map(parse).stream
      .buffer(1000, OverflowStrategy.backpressure)
      .map(EitherT.apply).asStreamT
      .map(cube)
      .mapAsync(5)(whiteListAsync).stream
      .map(_.subflatMap(identity))
      .throttle(1, 1.second).asStreamT
      .map(isEven).stream
      .map(_.value)
  }
}

object KafkaSource {
  // TODO: Use Source from State
  def apply(): Source[CommittableElement[Incoming], NotUsed] = Source.fromIterator(() => {
    var idx = 0

    new Iterator[CommittableElement[Incoming]] {
      override def hasNext = true
      override def next(): CommittableElement[Incoming] = {
        idx+=1

        val hexIdx = idx.toHexString

        (CommittableOffset(idx), Incoming(hexIdx))
      }
    }
  })
}
