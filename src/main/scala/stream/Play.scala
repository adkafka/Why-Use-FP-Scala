package stream

import akka.NotUsed
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.Source
import cats._
import cats.data._
import cats.implicits._
import stream.Play.{CommittableElement, CommittableOffset, Context, Incoming}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.language.higherKinds
import scala.util.Try

object Play {
  final case class Context[C, A](context: C, get: A) {
    def map[B](f: A => B): Context[C, B] = Context(context, f(get))
    def traverse[G[_]: Applicative, B](f: A => G[B]): G[Context[C, B]] = Applicative[G].map(f(get))(Context(context, _))
  }

  object Context {
    implicit def functor[C]: Functor[Context[C, ?]] = {
      new Functor[Context[C, ?]] {
        override def map[A, B](fa: Context[C, A])(f: (A) => B): Context[C, B] = Context(fa.context, f(fa.get))
      }
    }

    implicit def traverse[C]: Traverse[Context[C, ?]] = {
      new Traverse[Context[C, ?]] {
        override def traverse[G[_]: Applicative, A, B](fa: Context[C, A])(f: A => G[B]): G[Context[C, B]] = fa.traverse(f)
        override def foldLeft[A, B](fa: Context[C, A], b: B)(f: (B, A) => B): B = f(b, fa.get)
        // TODO: Make this function actually lazy
        override def foldRight[A, B](fa: Context[C, A], lb: Eval[B])(f: (A, Eval[B]) => Eval[B]): Eval[B] = f(fa.get, lb)
      }
    }
  }

  case class CommittableOffset(offset: Long)

  type CommittableElement[A] = Context[CommittableOffset, A]

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

  implicit val x = Applicative[Future]

  // Note: Would be syntactically cleaner with a StreamT/FlowT class
  val src: Source[CommittableElement[Either[Errors, Result]], NotUsed] = {
    KafkaSource()
      .map(c.map(_)(parse))
      .buffer(1000, OverflowStrategy.backpressure)
      .map(EitherT.apply)
      .map(_.map(cube))
      .mapAsync(5)(e => e.traverse(whiteListAsync)).map(_.subflatMap(identity))
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
