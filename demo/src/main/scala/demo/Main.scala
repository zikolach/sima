package demo

import cats.effect.kernel.Deferred
import cats.effect.std.Dispatcher
import cats.effect.unsafe.implicits.global
import cats.effect.{IO, IOApp, Resource}
import cats.syntax.all._
import com.typesafe.config.Config
import io.circe.syntax.EncoderOps
import org.apache.pekko.Done
import org.apache.pekko.actor.{CoordinatedShutdown, ActorSystem => UntypedActorSystem}
import org.apache.pekko.http.scaladsl.Http
import org.apache.pekko.http.scaladsl.model._
import org.apache.pekko.http.scaladsl.server.Directives._
import org.apache.pekko.http.scaladsl.server.RequestContext
import org.apache.pekko.stream.Materializer
import org.apache.pekko.stream.scaladsl.Source
import org.apache.pekko.util.ByteString
import org.slf4j.LoggerFactory
import sima.api.Parameters
import sima.{ClientConfig, SimaClient}

import scala.concurrent.duration.{Duration, _}
import scala.concurrent.{Future, TimeoutException}

//noinspection TypeAnnotation,SameParameterValue
object Main extends IOApp.Simple {

  /** Starts an (untyped) Akka actor system in the
   * context of a Cats-Effect `Resource`, and integrating
   * with its cancellation abilities.
   *
   * HINT: for apps (in `main`), it's best if
   * `akka.coordinated-shutdown.exit-jvm` is set to `on`,
   * because Akka can decide to shutdown on its own. And
   * having this setting interacts well with Cats-Effect.
   *
   * @param systemName                  is the identifying name of the system.
   * @param config                      is an optional, parsed HOCON configuration;
   *                                    if None, then Akka will read its own, possibly
   *                                    from `application.conf`; this parameter is
   *                                    provided in order to control the source of
   *                                    the application's configuration.
   * @param useIOExecutionContext       if true, then Cats-Effect's
   *                                    default thread-pool will get used by Akka, as well.
   *                                    This is needed in order to avoid having too many
   *                                    thread-pools.
   * @param timeoutAwaitCatsEffect      is the maximum amount of time
   *                                    Akka's coordinated-shutdown is allowed to wait for
   *                                    Cats-Effect to finish. This is needed, as Cats-Effect
   *                                    could have a faulty stack of disposables, or because
   *                                    Akka could decide to shutdown on its own.
   * @param timeoutAwaitAkkaTermination is the maximum amount of
   *                                    time to wait for the actor system to terminate, after
   *                                    `terminate()` was called. We need the timeout, because
   *                                    `terminate()` proved to return a `Future` that never
   *                                    completes in certain scenarios (could be a bug, or a
   *                                    race condition).
   * @see https://alexn.org/blog/2023/04/17/integrating-akka-with-cats-effect-3/#starting-actor-systems-as-a-resource
   */
  private def startActorSystemUntyped(
    systemName: String,
    config: Option[Config],
    useIOExecutionContext: Boolean,
    timeoutAwaitCatsEffect: Duration,
    timeoutAwaitAkkaTermination: Duration,
  ): Resource[IO, UntypedActorSystem] =
    // Needed to turn IO into Future
    // https://pekko.apache.org/docs/pekko/current/dispatchers.html
    Dispatcher.parallel[IO](await = true).flatMap { dispatcher =>
      Resource[IO, UntypedActorSystem](
        for {
          // Fishing IO's `ExecutionContext`
          ec <- Option
            .when(useIOExecutionContext)(IO.executionContext)
            .sequence
          // For synchronizing Cats-Effect with Akka
          awaitCancel <- Deferred[IO, Unit]
          // For awaiting termination via coordinated-shutdown,
          // needed as `terminate()` is unreliable
          awaitTermination <- Deferred[IO, Unit]
          logger = LoggerFactory.getLogger(getClass)
          system <- IO {
            logger.info("Creating actor system...")
            val system = UntypedActorSystem(
              systemName.trim.replaceAll("\\W+", "-"),
              config = config,
              defaultExecutionContext = ec,
            )
            // Registering task in Akka's CoordinatedShutdown
            // that will wait for Cats-Effect to catch up,
            // blocking Akka from terminating, see:
            // https://pekko.apache.org/docs/pekko/current/coordinated-shutdown.html
            CoordinatedShutdown(system).addTask(
              CoordinatedShutdown.PhaseBeforeServiceUnbind,
              "sync-with-cats-effect",
            ) { () =>
              dispatcher.unsafeToFuture(
                // WARN: this may not happen, if Akka decided
                // to terminate, and `coordinated-shutdown.exit-jvm`
                // isn't `on`, hence the timeout:
                awaitCancel.get
                  .timeout(timeoutAwaitCatsEffect)
                  .recoverWith {
                    case _: TimeoutException =>
                      IO(
                        logger.error(
                          "Timed out waiting for Cats-Effect to catch up! " +
                          "This might indicate either a non-terminating " +
                          "cancellation logic, or a misconfiguration of Akka."
                        )
                      )
                  }
                  .as(Done)
              )
            }
            CoordinatedShutdown(system).addTask(
              CoordinatedShutdown.PhaseActorSystemTerminate,
              "signal-actor-system-terminated",
            ) { () =>
              dispatcher.unsafeToFuture(
                awaitTermination.complete(()).as(Done)
              )
            }
            system
          }
        } yield {
          val cancel =
            for {
              // Signals that Cats-Effect has caught up with Akka
              _ <- awaitCancel.complete(())
              _ <- IO(logger.warn("Shutting down actor system!"))
              // Shuts down Akka, and waits for its termination
              // Here, system.terminate() returns a `Future[Terminated]`,
              // but we are ignoring it, as it could be non-terminating
              _ <- IO(system.terminate())
              // Waiting for Akka to terminate via coordinated-shutdown
              _ <- awaitTermination.get
              // WARN: `whenTerminated` is unreliable, hence the timeout
              _ <- IO
                .fromFuture(IO(system.whenTerminated))
                .void
                .timeoutAndForget(timeoutAwaitAkkaTermination)
                .handleErrorWith(
                  _ =>
                    IO(
                      logger.warn(
                        "Timed-out waiting for Akka to terminate!"
                      )
                  )
                )
            } yield ()
          (system, cancel)
        }
      )
    }

  implicit class SourceExt(val byteSource: Source[ByteString, _]) extends AnyVal {
    def toFuture(implicit ctx: RequestContext): Future[Array[Byte]] = {
      implicit val mat: Materializer = ctx.materializer
      byteSource
        .map(_.iterator.toArray)
        .runFold(Array.empty[Byte])((acc, next) => acc.appendedAll(next))
    }
  }

  implicit val config: ClientConfig = sima.ClientConfig()
  private val route =
  path("form") {
    get {
      complete(templates.MainPage())
    }
  } ~
  (path("health") & get) {
    complete {
      SimaClient
        .client[IO]
        .use(_.health)
        .map { b =>
          HttpEntity(b.toString)
        }
        .unsafeToFuture()
    }
  } ~
  (path("info") & post) {
    fileUpload("file") {
      case (_, byteSource) =>
        extractRequestContext { implicit ctx =>
          onSuccess(byteSource.toFuture) { bytes =>
            complete {
              SimaClient
                .client[IO]
                .use(_.info(bytes))
                .map { info =>
                  HttpEntity(ContentTypes.`application/json`, info.asJson.noSpaces)
                }
                .unsafeToFuture()
            }
          }
        }
    }
  } ~
  (path("resize") & post) {
    fileUpload("file") {
      case (_, byteSource) =>
        extractRequestContext { implicit ctx =>
          onSuccess(byteSource.toFuture) { bytes =>
            complete {
              SimaClient
                .client[IO]
                .use(_.resize(bytes)(Parameters(`type` = Some("png"), width = Some(200))))
                .map { resized =>
                  HttpEntity(ContentType(MediaTypes.`image/png`), resized)
                }
                .unsafeToFuture()
            }
          }
        }
    }
  }

  override def run: IO[Unit] =
    startActorSystemUntyped(
      systemName = "my-system",
      config = None,
      useIOExecutionContext = true,
      timeoutAwaitCatsEffect = 30.seconds,
      timeoutAwaitAkkaTermination = 30.seconds
    ).use { implicit system =>
      IO.fromFuture(IO(Http().newServerAt("localhost", 9000).bind(route))).flatMap { binding =>
        IO.println(s"Server now online. Please navigate to http://localhost:9000/form\nPress RETURN to stop...") >>
        IO.readLine >> // let it run until user presses return
        IO.fromFuture(IO(binding.unbind())) >> // trigger unbinding from the port
        IO.fromFuture(IO(system.terminate())).void // and shutdown when done
      }
    }
}
