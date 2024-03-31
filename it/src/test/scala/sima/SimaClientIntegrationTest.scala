package sima

import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec
import com.dimafeng.testcontainers.GenericContainer
import com.dimafeng.testcontainers.scalatest.TestContainerForAll
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should
import org.testcontainers.containers.wait.strategy.Wait
import sima.api.{ImageInfo, Parameters}

import java.nio.file.{Files, Path}

//noinspection TypeAnnotation
class SimaClientIntegrationTest extends AsyncFlatSpec with AsyncIOSpec with should.Matchers with TestContainerForAll {
  override val containerDef = GenericContainer.Def(
    dockerImage = "h2non/imaginary:latest",
    exposedPorts = Seq(8080),
    env = Map("PORT" -> "8080"),
    command = Seq("-enable-url-source"),
    waitStrategy = Wait.forHttp("/health")
  )

  //noinspection HttpUrlsUsage
  implicit def serverToConfig(implicit server: Containers): ClientConfig = {
    val s = server.asInstanceOf[GenericContainer]
    val baseUrl = s"http://${s.containerIpAddress}:${s.mappedPort(8080)}"
    ClientConfig(baseUrl)
  }

  behavior of "SimaClient"

  it should "request service status" in {
    withContainers { implicit server =>
      SimaClient.client[IO].use(_.health).asserting { result =>
        result should be(true)
      }
    }
  }

  it should "request image info" in {
    withContainers { implicit server =>
      val image = getClass.getResourceAsStream("test.webp").readAllBytes()
      println(image.size)
      SimaClient.client[IO].use(_.info(image)).asserting { result =>
        result should be(ImageInfo(100, 100, "webp", "srgb", hasAlpha = false, hasProfile = false, 3, 1))
      }
    }
  }

  it should "request image resize" in {
    withContainers { implicit server =>
      val image = getClass.getResourceAsStream("test.webp").readAllBytes()
      SimaClient.client[IO].use(_.resize(image)(Parameters(width = Some(20)))).asserting { result =>
        Files.write(Path.of("images/out.webp"), result)
        result.length should be(360)
      }
    }
  }

  it should "convert svg to png" in {
    withContainers { implicit server =>
      val image = getClass.getResourceAsStream("debian.svg").readAllBytes()
      SimaClient.client[IO].use(_.convert(image)(Parameters(`type` = Some("png")))).asserting { result =>
        Files.write(Path.of("images/out.png"), result)
        new String(result.slice(1, 4)) should be("PNG")
      }
    }
  }

  it should "convert svg to png and fit" in {
    withContainers { implicit server =>
      val image = getClass.getResourceAsStream("debian.svg").readAllBytes()
      SimaClient.client[IO].use(_.convert(image)(Parameters(`type` = Some("png"), width = Some(50)))).asserting { result =>
        Files.write(Path.of("images/fit.png"), result)
        new String(result.slice(1, 4)) should be("PNG")
      }
    }
  }

  it should "do multiple operations" in {
    withContainers { implicit server =>
      SimaClient.client[IO].use { sc =>
        for {
          image   <- IO.blocking(getClass.getResourceAsStream("debian.svg").readAllBytes())
          healthy <- sc.health
          info    <- sc.info(image)
        } yield healthy -> info
      } asserting {
        case (h, i) =>
          h should be(true)
          i.width should be(100)
      }
    }
  }
}
