import api.ImageInfo
import cats.effect.IO
import io.circe.parser._
import sttp.client3.{asByteArrayAlways, asStringAlways, basicRequest, UriContext}
import sttp.client3.httpclient.cats.HttpClientCatsBackend

abstract class SimaClient[F[_]](config: ClientConfig) {
  def health: F[Boolean]
  def info(imageData: Array[Byte]): IO[ImageInfo]
  def resize(imageData: Array[Byte])(width: Int): IO[Array[Byte]]

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
            case x            => IO.raiseError(new Exception(s"Could not process request: status ${x.code}\n${new String(x.body)}"))
          }
      }

    override def resize(imageData: Array[Byte])(width: Int): IO[Array[Byte]] =
      HttpClientCatsBackend.resource[IO]().use { backend =>
        val req = basicRequest
          .post(uri"${config.baseUrl}/resize".addParam("width", width.toString))
          .body(imageData)
          .response(asByteArrayAlways)
        println(req.toCurl)
        req
          .send(backend)
          .flatMap {
            case x if x.is200 => IO.pure(x.body)
            case x            => IO.raiseError(new Exception(s"Could not process request: status ${x.code}\n${new String(x.body)}"))
          }
      }

  }
}
