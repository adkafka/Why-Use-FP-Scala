package stream

import akka.NotUsed
import akka.stream.scaladsl.Source
import cats._
import cats.data._
import stream.Play.{CommittableElement, CommittableOffset, Context, Incoming}

import scala.util.Try

object Play {
  final case class Context[C, A](context: C, get: A) {
    def map[B](f: A => B): Context[C, B] = Context(context, f(get))
  }

  object Context {
    implicit def functor[C, A] = {
      type ContextC[A_] = Context[C, A_]
      new Functor[ContextC] {
        override def map[A, B](fa: ContextC[A])(f: (A) => B) = fa.map(f)
      }
    }
  }

  case class CommittableOffset(offset: Long)

  type CommittableElement[A] = Context[CommittableOffset, A]

  val c: Functor[CommittableElement] = Functor[CommittableElement]
  val ce = EitherT[CommittableElement, Errors, Result]


  case class Incoming(hexString: String)
  case class Parsed(num: Int)
  case class Cubed(num: Int)
  case class Result(isEven: Boolean)

  sealed trait Errors
  case class ParseError(input: Incoming) extends Errors

  KafkaSource()
    .map(c.map(_)(parse))
    .map(EitherT.apply)
    .map(_.map(cube))
    .map(_.map(isEven))

  def parse(in: Incoming): Either[Errors, Parsed] = {
    Try(in.hexString.toInt).fold(
      _ => Left(ParseError(in)),
      int => Right(Parsed(int))
    )
  }
  def cube(p: Parsed): Cubed = Cubed(p.num ^ 3)
  def isEven(p: Cubed): Result = if (p.num % 2 == 0) Result(true) else Result(false)
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
