package stream.syntax

object TupleExt {
  implicit class Tuple2Ext[A, B](tuple: (A, B)) {
    def left: A = tuple._1
    def right: B = tuple._2
  }
}
