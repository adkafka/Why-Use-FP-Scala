package stream.syntax

import akka.stream.scaladsl.Source
import stream.StreamT

object SourceExt {
  implicit class SourceTExt[F[_], A, M](source: Source[F[A], M]) {
    def asStreamT: StreamT[F, A, M] = StreamT(source)
  }
}
