package api

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Sink
import cats.data.EitherT
import cats.implicits._
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import io.circe.generic.auto._

import scala.util.{Failure, Success}

/* TODO: Need a:
 * def mapAsyncE[T](par: Int)(f: Out ⇒ T): Repr[Either[Throwable, T]]
 * def mapAsyncE[T, Ex](par: Int)(f: Out ⇒ T): Repr[Ex[Throwable, T]]
 */

import scala.concurrent.Future

object Api {
  implicit val sys = ActorSystem()
  implicit val mat = ActorMaterializer()
  implicit val ec  = sys.dispatcher

  sealed trait ApiError extends Exception
  case class HttpConnectionException(value: Throwable) extends ApiError
  case class HttpResponseException(code: StatusCode) extends ApiError
  case class UnmarshalResponseException(value: Throwable) extends ApiError

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
            .recoverWith { case ex => Future.failed(UnmarshalResponseException(ex)) }
      }
      .recoverWith { case ex: Throwable => Future.failed(HttpConnectionException(ex)) }
  }

  def doThings2(uri: Uri): Future[Response] = {
    import cats.implicits._

    final case class SuccessResponse(get: HttpResponse)

    def filterBadResponse(response: HttpResponse): Future[SuccessResponse] = {
      if (response.status != StatusCodes.OK) {
        response
          .entity
          .dataBytes
          .runWith(Sink.ignore)
          .flatMap(_ => Future.failed(HttpResponseException(response.status)))
      }
      else Future.successful(SuccessResponse(response))
    }

    Http().singleRequest(HttpRequest(uri = uri))
      .adaptError { case ex: Throwable => HttpConnectionException(ex) }
      .flatMap(filterBadResponse)
      .flatMap(goodResponse => Unmarshal(goodResponse.get).to[Response])
      .adaptError { case ex: Throwable => UnmarshalResponseException(ex) }
  }

  def doThings3(uri: Uri): Future[Either[ApiError, Response]] = {
    import cats.implicits._

    type FutApi[T] = Future[Either[ApiError, T]]
    type FutApiT[T] = EitherT[Future, ApiError, T]

    final case class SuccessResponse(get: HttpResponse)

    // TODO: This seems like re-inventing the wheel... cats must have something for this
    def liftToEither[E, A](future: Future[A])(lift: PartialFunction[Throwable, E]): Future[Either[E, A]] = {
      future
        .map(Either.right)
        .recover(lift.andThen(Either.left))
    }

    def request(): FutApi[HttpResponse] = {
      liftToEither(Http().singleRequest(HttpRequest(uri = uri))){ case ex: Throwable => HttpConnectionException(ex) }
    }

    def filterBadResponse(response: HttpResponse): FutApi[SuccessResponse] = {
      if (response.status != StatusCodes.OK) {
        response
          .entity
          .dataBytes
          .runWith(Sink.ignore)
          .flatMap(_ => Future.successful(Either.left(HttpResponseException(response.status))))
      }
      else Future.successful(Either.right(SuccessResponse(response)))
    }

    EitherT(request())
      .flatMapF(filterBadResponse)
      .flatMapF { goodResponse =>
        liftToEither(Unmarshal(goodResponse.get).to[Response]) { case ex: Throwable => UnmarshalResponseException(ex) }
      }
      .value
  }
}