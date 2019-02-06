package stream

import akka.stream.scaladsl.Source
import cats._
import cats.implicits._

import scala.concurrent.{ExecutionContext, Future}
import scala.language.higherKinds

/* TODO:
 * * Implement a sameThread EC, use that for Async calls
 * * Deal with Either
 * * subflatmap?
 */


final case class StreamT[F[_], A, M](stream: Source[F[A], M]) extends AnyVal {
  def map[B](f: A => B)(implicit F: Functor[F]): StreamT[F, B, M] = StreamT(stream.map(F.map(_)(f)))
  def mapAsync[B](parallelism: Int)(f: A => Future[B])(implicit F: Traverse[F], ec: ExecutionContext): StreamT[F, B, M] = {
    StreamT(stream.mapAsync(parallelism)(F.traverse(_)(f)))
  }
  def mapAsyncUnordered[B](parallelism: Int)(f: A => Future[B])(implicit F: Traverse[F], ec: ExecutionContext): StreamT[F, B, M] = {
    StreamT(stream.mapAsyncUnordered(parallelism)(F.traverse(_)(f)))
  }

  //subflatmap or something to deal with either
}
