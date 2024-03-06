import api.{ImageInfo, Parameters}
import cats.effect.IO
import io.circe.parser._
import io.circe.syntax._
import sttp.client3.{asByteArrayAlways, asStringAlways, basicRequest, Response, UriContext}
import sttp.client3.httpclient.cats.HttpClientCatsBackend

abstract class SimaClient[F[_]](config: ClientConfig) {
  def health: F[Boolean]
  def info(imageData: Array[Byte]): IO[ImageInfo]
  def resize(imageData: Array[Byte])(params: Parameters): IO[Array[Byte]]
  def convert(imageData: Array[Byte])(params: Parameters): IO[Array[Byte]]
  def fit(imageData: Array[Byte])(params: Parameters): IO[Array[Byte]]

}

object SimaClient {
  def client(implicit config: ClientConfig): SimaClient[IO] = new SimaClient[IO](config) {
    override def health: IO[Boolean] =
      HttpClientCatsBackend.resource[IO]().use { backend =>
        basicRequest.get(uri"${config.baseUrl}/health").response(asStringAlways).send(backend).map {
          case x if x.is200 => true
          case _            => false
        }
      }

    override def info(imageData: Array[Byte]): IO[ImageInfo] =
      HttpClientCatsBackend.resource[IO]().use { backend =>
        basicRequest
          .post(uri"${config.baseUrl}/info")
          .body(imageData)
          .response(asStringAlways)
          .send(backend)
          .flatMap {
            case x if x.is200 => IO.fromEither(decode[ImageInfo](x.body))
            case x            => handleError(x)
          }
      }

    override def resize(imageData: Array[Byte])(params: Parameters): IO[Array[Byte]] =
      HttpClientCatsBackend.resource[IO]().use { backend =>
        basicRequest
          .post(uri"${config.baseUrl}/resize".addParams(params.toQuery(Parameters.resize)))
          .body(imageData)
          .response(asByteArrayAlways)
          .send(backend)
          .flatMap {
            case x if x.is200 => IO.pure(x.body)
            case x            => handleError(x)
          }
      }

    override def convert(imageData: Array[Byte])(params: Parameters): IO[Array[Byte]] =
      HttpClientCatsBackend.resource[IO]().use { backend =>
        basicRequest
          .post(uri"${config.baseUrl}/convert".addParams(params.toQuery(Parameters.convert)))
          .body(imageData)
          .response(asByteArrayAlways)
          .send(backend)
          .flatMap {
            case x if x.is200 => IO.pure(x.body)
            case x            => handleError(x)
          }
      }

    override def fit(imageData: Array[Byte])(params: Parameters): IO[Array[Byte]] =
      HttpClientCatsBackend.resource[IO]().use { backend =>
        basicRequest
          .post(uri"${config.baseUrl}/convert".addParams(params.toQuery(Parameters.fit)))
          .body(imageData)
          .response(asByteArrayAlways)
          .send(backend)
          .flatMap {
            case x if x.is200 => IO.pure(x.body)
            case x            => handleError(x)
          }
      }
  }

  private def handleError[T, B](x: Response[B]): IO[T] = {
    val body = x.body match {
      case x: String      => x
      case x: Array[Byte] => new String(x)
      case x              => x.toString
    }
    IO.raiseError(new Exception(s"Could not process request: status ${x.code}\n${body}"))
  }
}
