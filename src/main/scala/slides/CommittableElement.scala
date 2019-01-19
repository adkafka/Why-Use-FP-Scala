package slides

import akka.kafka.ConsumerMessage.CommittableOffset
import cats._
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
}
