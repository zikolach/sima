package sima

import cats.implicits._
import cats.effect.{Async, Resource}
import io.circe.parser._
import sima.api.{ImageInfo, Parameters}
import sttp.client3.{Response, UriContext, asByteArrayAlways, asStringAlways, basicRequest}
import sttp.client3.httpclient.cats.HttpClientCatsBackend

trait SimaClient[F[_]] {
  def health: F[Boolean]
  def info(imageData: Array[Byte]): F[ImageInfo]
  def resize(imageData: Array[Byte])(params: Parameters): F[Array[Byte]]
  def convert(imageData: Array[Byte])(params: Parameters): F[Array[Byte]]
  def fit(imageData: Array[Byte])(params: Parameters): F[Array[Byte]]

}

object SimaClient {
  def client[F[_]: Async](implicit config: ClientConfig): Resource[F, SimaClient[F]] =
    HttpClientCatsBackend.resource[F]().map { backend =>
      new SimaClient[F] {
        override def health: F[Boolean] =
          basicRequest.get(uri"${config.baseUrl}/health").response(asStringAlways).send(backend).map {
            case x if x.is200 => true
            case _            => false
          }

        override def info(imageData: Array[Byte]): F[ImageInfo] =
          basicRequest
            .post(uri"${config.baseUrl}/info")
            .body(imageData)
            .response(asStringAlways)
            .send(backend)
            .flatMap {
              case x if x.is200 => decode[ImageInfo](x.body).liftTo[F]
              case x            => handleError(x)
            }

        override def resize(imageData: Array[Byte])(params: Parameters): F[Array[Byte]] =
          basicRequest
            .post(uri"${config.baseUrl}/resize".addParams(params.toQuery(Parameters.resize)))
            .body(imageData)
            .response(asByteArrayAlways)
            .send(backend)
            .flatMap {
              case x if x.is200 => x.body.pure[F]
              case x            => handleError(x)
            }

        override def convert(imageData: Array[Byte])(params: Parameters): F[Array[Byte]] =
          basicRequest
            .post(uri"${config.baseUrl}/convert".addParams(params.toQuery(Parameters.convert)))
            .body(imageData)
            .response(asByteArrayAlways)
            .send(backend)
            .flatMap {
              case x if x.is200 => x.body.pure[F]
              case x            => handleError(x)
            }

        override def fit(imageData: Array[Byte])(params: Parameters): F[Array[Byte]] =
          basicRequest
            .post(uri"${config.baseUrl}/convert".addParams(params.toQuery(Parameters.fit)))
            .body(imageData)
            .response(asByteArrayAlways)
            .send(backend)
            .flatMap {
              case x if x.is200 => x.body.pure[F]
              case x            => handleError(x)
            }
      }
    }

  private def handleError[F[_]: Async, B, R](x: Response[B]): F[R] = {
    val body = x.body match {
      case x: String      => x
      case x: Array[Byte] => new String(x)
      case x              => x.toString
    }
    new Exception(s"Could not process request ${x.request.uri}: status ${x.code}\n$body").raiseError[F, R]
  }
}
