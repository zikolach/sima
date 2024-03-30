package demo

import cats.effect.IO
import io.circe.syntax.EncoderOps
import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.http.scaladsl.Http
import org.apache.pekko.http.scaladsl.model._
import org.apache.pekko.http.scaladsl.server.Directives._
import org.apache.pekko.stream.Materializer
import sima.SimaClient
import sima.api.Parameters

import scala.concurrent.Future
import scala.io.StdIn

//noinspection TypeAnnotation
object Main {
  def main(args: Array[String]): Unit = {

    implicit val system = ActorSystem(Behaviors.empty, "my-system")
    // needed for the future flatMap/onComplete in the end
    implicit val executionContext = system.executionContext

    val route =
    path("form") {
      get {
        complete(templates.MainPage.apply())
      }
    } ~ path("info") {
      get {
        complete {
          import cats.effect.unsafe.implicits.global
          implicit val config = sima.ClientConfig()
          SimaClient.client[IO].use(_.health).unsafeToFuture().map { b =>
            HttpEntity(b.toString)
          }
        }
      } ~ post {
        fileUpload("file") {
          case (metadata, byteSource) =>
            extractRequestContext { ctx =>
              implicit val mat: Materializer = ctx.materializer
              val fileBytes: Future[Array[Byte]] = byteSource.map(_.iterator.toArray).runFold(Array.empty[Byte])((acc, next) => acc.appendedAll(next))
              onSuccess(fileBytes) { bytes =>
                complete {
                  import cats.effect.unsafe.implicits.global
                  implicit val config = sima.ClientConfig()
                  SimaClient.client[IO].use(_.info(bytes)).unsafeToFuture().map { info =>
                    HttpEntity(ContentTypes.`application/json`, info.asJson.noSpaces)
                  }
                }
              }
            }
        }
      }
    } ~ path("resize") {
      get {
        complete {
          import cats.effect.unsafe.implicits.global
          implicit val config = sima.ClientConfig()
          SimaClient.client[IO].use(_.health).unsafeToFuture().map { b =>
            HttpEntity(b.toString)
          }
        }
      } ~ post {
        fileUpload("file") {
          case (metadata, byteSource) =>
            extractRequestContext { ctx =>
              implicit val mat: Materializer = ctx.materializer
              val fileBytes: Future[Array[Byte]] = byteSource.map(_.iterator.toArray).runFold(Array.empty[Byte])((acc, next) => acc.appendedAll(next))
              onSuccess(fileBytes) { bytes =>
                complete {
                  import cats.effect.unsafe.implicits.global
                  implicit val config = sima.ClientConfig()
                  SimaClient.client[IO].use(_.resize(bytes)(Parameters(`type` = Some("png"), width = Some(200)))).unsafeToFuture().map { resized =>
                    HttpEntity(ContentType(MediaTypes.`image/png`), resized)
                  }
                }
              }
            }
        }
      }
    }

    val bindingFuture = Http().newServerAt("localhost", 9000).bind(route)

    println(s"Server now online. Please navigate to http://localhost:9000/form\nPress RETURN to stop...")
    StdIn.readLine() // let it run until user presses return
    bindingFuture
      .flatMap(_.unbind()) // trigger unbinding from the port
      .onComplete(_ => system.terminate()) // and shutdown when done
  }
}
