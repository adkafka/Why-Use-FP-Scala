package stream

import akka.stream.scaladsl.Source
import cats._
import cats.implicits._

import scala.concurrent.{ExecutionContext, Future}

/* TODO:
 * * Implement a sameThread EC, use that for Async calls
 * * Flow?
 */

/**
  * Wrapper for Source[F[A], M]
  */
final case class StreamT[F[_], A, M](stream: Source[F[A], M]) {
  // Replace with real sameThreadExecutionContext
  implicit val sameThreadExecutionContext: ExecutionContext = scala.concurrent.ExecutionContext.global

  def transform[B, MM](f: Source[F[A], M] => Source[F[B], MM]): StreamT[F, B, MM] = StreamT(f(stream))

  def map[B](f: A => B)(implicit F: Functor[F]): StreamT[F, B, M] = StreamT(stream.map(F.map(_)(f)))

  def mapAsync[B](parallelism: Int)(f: A => Future[B])(implicit F: Traverse[F]): StreamT[F, B, M] = {
    StreamT(stream.mapAsync(parallelism)(F.traverse(_)(f)))
  }

  def mapAsyncUnordered[B](parallelism: Int)(f: A => Future[B])(implicit F: Traverse[F]): StreamT[F, B, M] = {
    StreamT(stream.mapAsyncUnordered(parallelism)(F.traverse(_)(f)))
  }

  def subflatMap[B](f: A => F[B])(implicit F: Monad[F]): StreamT[F, B, M] =  transform(_.map(F.flatMap(_)(f)))
}
