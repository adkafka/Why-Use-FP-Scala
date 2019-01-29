package slides

import akka.kafka.ConsumerMessage.CommittableOffset
import cats._
import cats.data.EitherT
import cats.implicits._

object CommittableElement {
  implicit val functor = new Functor[CommittableElement] {
    override def map[A, B](fa: CommittableElement[A])(f: A => B): CommittableElement[B] = {
      CommittableElement(fa.committableOffset, f(fa.element))
    }
  }
}

case class CommittableElement[E](committableOffset: CommittableOffset, element: E)

object Main extends App {
  CommittableElement(null, 6).map(_ + 1)

  def greet(str: String): Either[Throwable, String] = {
    if (str.contains("boom")) Left(new RuntimeException("boom"))
    else Right(s"$str world")
  }

  val nestedHello = CommittableElement(null, Either.right[Throwable, String]("hello"))
  val nestedBoom = CommittableElement(null, Either.right[Throwable, String]("boom"))

  EitherT(nestedHello).map(_.toUpperCase).subflatMap(greet)
  EitherT(nestedBoom).subflatMap(greet)
}
