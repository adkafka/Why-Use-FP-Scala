package stream

import akka.NotUsed
import akka.stream.scaladsl.{Flow, Keep, Source}

/**
  * Wrapper for type Flow[F[A], F[B], M]
  */

object FlowT{
  def apply[F[_], A, B, M](flow: Flow[F[A], F[B], M]): FlowT[F, A, B, M] = FlowT(_.viaMat(flow)(Keep.right))
}

final case class FlowT[F[_], A, B, M](f: Source[F[A], _] => Source[F[B], M]) {
  // FlatMapConcat is quite slow, might break things too (throttle)
  def asFlow: Flow[F[A], F[B], NotUsed] = Flow[F[A]].flatMapConcat(x => f(Source.single(x)))

  def compose[C, MM](g: Source[F[B], M] => Source[F[C], MM]): FlowT[F, A, C, MM] = FlowT(f.andThen(g))

  def composeT[C, MM](g: StreamT[F, B, M] => StreamT[F, C, MM]): FlowT[F, A, C, MM] = {
    FlowT(f.andThen(x => g(StreamT(x)).stream))
  }
}
