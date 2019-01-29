package api

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import io.circe.generic.auto._
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._

import scala.concurrent.Future

object Api {
  implicit val sys = ActorSystem()
  implicit val mat = ActorMaterializer()
  implicit val ec  = sys.dispatcher

  case class HttpConnectionException(value: Any) extends Exception
  case class HttpResponseException(code: StatusCode) extends Exception
  case class UnmarshalResponseException(value: Any) extends Exception

  case class Response(content: String, time: Long, value: Double)

  def doThings(uri: Uri): Future[Response] = {
    Http()
      .singleRequest(HttpRequest(uri = uri))
      .flatMap {
        case response if response.status != StatusCodes.OK =>
          response.entity.dataBytes.runWith(Sink.ignore)
          Future.failed(HttpResponseException(response.status))
        case response =>
          Unmarshal(response)
            .to[Response]
            .recoverWith {
              case ex =>
                Future.failed(UnmarshalResponseException(ex.getMessage))
            }
      }
      .recoverWith {
        case ex => Future.failed(HttpConnectionException(ex.getMessage))
      }
  }

  def doThings2(uri: Uri): Future[Response] = {
    import cats.data._
    import cats.implicits._
    import cats.syntax._

    /* TODO: Need a:
     * def mapAsyncE[T](par: Int)(f: Out ⇒ T): Repr[Either[Throwable, T]]
     * def mapAsyncE[T, Ex](par: Int)(f: Out ⇒ T): Repr[Ex[Throwable, T]]
     */


    Source.fromFuture(Http().singleRequest(HttpRequest(uri = uri)))
      .map {
        case response if response.status != StatusCodes.OK =>
          response.entity.dataBytes.runWith(Sink.ignore)
          Left(HttpResponseException(response.status))
        case r => Right(r)
      }
      .map { x =>
        x.map { el =>
          Unmarshal(el)
            .to[Response]
            .map(Either.right)
            .recover { case e => Future.successful(Either.left(e)) }
        }
      }
  }
}
