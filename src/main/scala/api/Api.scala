package api

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Sink
import cats._
import cats.data._
import cats.implicits._
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import io.circe.generic.auto._

import scala.concurrent.ExecutionContext

/* TODO: Need a:
 * def mapAsyncE[T](par: Int)(f: Out ⇒ T): Repr[Either[Throwable, T]]
 * def mapAsyncE[T, Ex](par: Int)(f: Out ⇒ T): Repr[Ex[Throwable, T]]
 */

import scala.concurrent.Future

object Api {
  implicit val sys: ActorSystem = ActorSystem()
  implicit val mat: ActorMaterializer = ActorMaterializer()
  implicit val ec: ExecutionContext = sys.dispatcher

  sealed trait ApiError extends Exception
  case class HttpConnectionException(value: Throwable) extends ApiError
  case class HttpResponseException(code: StatusCode) extends ApiError
  case class UnmarshalResponseException(value: Throwable) extends ApiError

  case class Response(content: String, time: Long, value: Double)

  def doThings(uri: Uri): Future[Response] = {
    Http().singleRequest(HttpRequest(uri = uri))
      .flatMap {
        case response if response.status != StatusCodes.OK =>
          response.entity.dataBytes.runWith(Sink.ignore).flatMap { _ =>
            Future.failed(HttpResponseException(response.status))
          }
        case response =>
          Unmarshal(response)
            .to[Response]
            .recoverWith { case ex => Future.failed(UnmarshalResponseException(ex)) }
      }
      .recoverWith { case ex => Future.failed(HttpConnectionException(ex)) }
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
      .adaptError { case ex => HttpConnectionException(ex) }
      .flatMap(filterBadResponse)
      .flatMap(goodResponse => Unmarshal(goodResponse.get).to[Response])
      .adaptError { case ex => UnmarshalResponseException(ex) }
  }

  object MyFutureOps {
    class RichFutureOps[A](future: Future[A]) {
      // TODO: This seems like re-inventing the wheel... but cats doesn't seem to have this yet
      def liftToEither[E](liftWith: PartialFunction[Throwable, E])(implicit ec: ExecutionContext): Future[Either[E, A]] = {
        future
          .map(_.asRight)
          .recover(liftWith.andThen(_.asLeft))
      }
    }

    implicit def richFuture[A](f: Future[A])(implicit ec: ExecutionContext): RichFutureOps[A] = new RichFutureOps(f)
  }

  def doThings3(uri: Uri): Future[Either[ApiError, Response]] = {
    import MyFutureOps._

    type EitherError[T] = Either[ApiError, T]
    type FutApi[T] = Future[EitherError[T]]
    implicit val x: Applicative[FutApi] = Applicative[Future].compose[Either[ApiError, ?]]

    final case class SuccessResponse(get: HttpResponse)

    def request(): FutApi[HttpResponse] = {
      Http().singleRequest(HttpRequest(uri = uri))
        .liftToEither { case ex => HttpConnectionException(ex) }
    }

    def filterBadResponse(response: HttpResponse): FutApi[SuccessResponse] = {
      if (response.status != StatusCodes.OK) {
        response
          .entity
          .dataBytes
          .runWith(Sink.ignore)
          .flatMap(_ => Future.successful(Either.left(HttpResponseException(response.status))))
      }
      else SuccessResponse(response).pure[FutApi]
    }

    def unmarshal(goodResponse: SuccessResponse): FutApi[Response] = {
      Unmarshal(goodResponse.get).to[Response]
        .liftToEither { case ex => UnmarshalResponseException(ex) }
    }

    EitherT(request())
      .flatMapF(filterBadResponse)
      .flatMapF(unmarshal)
      .value
  }
}
