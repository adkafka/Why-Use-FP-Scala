package stream

import akka.NotUsed
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.Source
import cats._
import cats.data._
import stream.Play.{CommittableElement, CommittableOffset, Context, Incoming}

import scala.util.Try
import scala.concurrent.duration._

object Play {
  final case class Context[C, A](context: C, get: A)

  object Context {
    implicit def functor[C]: Functor[Context[C, ?]] = {
      new Functor[Context[C, ?]] {
        override def map[A, B](fa: Context[C, A])(f: (A) => B): Context[C, B] = Context(fa.context, f(fa.get))
      }
    }
  }

  case class CommittableOffset(offset: Long)

  type CommittableElement[A] = Context[CommittableOffset, A]

  val c: Functor[CommittableElement] = Functor[CommittableElement]

  case class Incoming(hexString: String)
  case class Parsed(num: Int)
  case class Cubed(num: Int)
  case class Result(isEven: Boolean)

  sealed trait Errors
  case class ParseError(input: Incoming) extends Errors

  def parse(in: Incoming): Either[Errors, Parsed] = {
    Try(in.hexString.toInt).fold(
      _ => Left(ParseError(in)),
      int => Right(Parsed(int))
    )
  }
  def cube(p: Parsed): Cubed = Cubed(p.num ^ 3)
  def isEven(p: Cubed): Result = if (p.num % 2 == 0) Result(true) else Result(false)

  val src: Source[CommittableElement[Either[Errors, Result]], NotUsed] = {
    KafkaSource()
      .map(c.map(_)(parse))
      .buffer(1000, OverflowStrategy.backpressure)
      .map(EitherT.apply)
      .map(_.map(cube))
      .throttle(1, 1.second)
      .map(_.map(isEven))
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

        Context(CommittableOffset(idx), Incoming(hexIdx))
      }
    }
  })
}
