package sima

import cats.implicits._
import cats.effect.{Async, Resource}
import io.circe.Decoder
import io.circe.parser._
import sima.api.{ImageInfo, Parameters}
import sttp.client3.{asByteArrayAlways, asStringAlways, basicRequest, Response, UriContext}
import sttp.client3.httpclient.cats.HttpClientCatsBackend
import sttp.model.Uri

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
          basicRequest
            .get(uri"${config.baseUrl}/health")
            .response(asStringAlways)
            .send(backend)
            .map(_.is200)

        private def postImageData[R](url: Uri, imageData: Array[Byte])(implicit decoder: Decoder[R]): F[R] =
          postImageDataRaw(url, imageData).flatMap(x => decode[R](new String(x)).liftTo[F])

        private def postImageDataRaw(url: Uri, imageData: Array[Byte]): F[Array[Byte]] =
          basicRequest
            .post(url)
            .body(imageData)
            .response(asByteArrayAlways)
            .send(backend)
            .flatMap {
              case x if x.is200 => x.body.pure[F]
              case x            => handleError(x)
            }

        override def info(imageData: Array[Byte]): F[ImageInfo] =
          postImageData(uri"${config.baseUrl}/info", imageData)

        override def resize(imageData: Array[Byte])(params: Parameters): F[Array[Byte]] =
          postImageDataRaw(uri"${config.baseUrl}/resize".addParams(params.toQuery(Parameters.resize)), imageData)

        override def convert(imageData: Array[Byte])(params: Parameters): F[Array[Byte]] =
          postImageDataRaw(uri"${config.baseUrl}/convert".addParams(params.toQuery(Parameters.convert)), imageData)

        override def fit(imageData: Array[Byte])(params: Parameters): F[Array[Byte]] =
          postImageDataRaw(uri"${config.baseUrl}/fit".addParams(params.toQuery(Parameters.fit)), imageData)
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
