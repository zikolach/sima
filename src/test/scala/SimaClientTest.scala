import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should
import sima.{ClientConfig, SimaClient}
import sima.api.{ImageInfo, Parameters}

import java.nio.file.{Files, Path}

class SimaClientTest extends AsyncFlatSpec with AsyncIOSpec with should.Matchers {
  behavior of "SimaClient"

  it should "request service status" in {
    implicit val clientConfig: ClientConfig = ClientConfig()
    SimaClient.client[IO].use(_.health).asserting { result =>
      result should be(true)
    }
  }

  it should "request image info" in {
    val image = getClass.getResourceAsStream("test.webp").readAllBytes()
    println(image.size)
    implicit val clientConfig: ClientConfig = ClientConfig()
    SimaClient.client[IO].use(_.info(image)).asserting { result =>
      result should be(ImageInfo(100, 100, "webp", "srgb", hasAlpha = false, hasProfile = false, 3, 1))
    }
  }

  it should "request image resize" in {
    val image = getClass.getResourceAsStream("test.webp").readAllBytes()
    implicit val clientConfig: ClientConfig = ClientConfig()
    SimaClient.client[IO].use(_.resize(image)(Parameters(width = Some(20)))).asserting { result =>
      Files.write(Path.of("images/out.webp"), result)
      result.length should be(360)
    }
  }

  it should "convert svg to png" in {
    val image = getClass.getResourceAsStream("debian.svg").readAllBytes()
    implicit val clientConfig: ClientConfig = ClientConfig()
    SimaClient.client[IO].use(_.convert(image)(Parameters(`type` = Some("png")))).asserting { result =>
      Files.write(Path.of("images/out.png"), result)
      new String(result.slice(1, 4)) should be("PNG")
    }
  }

  it should "convert svg to png and fit" in {
    val image = getClass.getResourceAsStream("debian.svg").readAllBytes()
    implicit val clientConfig: ClientConfig = ClientConfig()
    SimaClient.client[IO].use(_.convert(image)(Parameters(`type` = Some("png"), width = Some(50)))).asserting { result =>
      Files.write(Path.of("images/fit.png"), result)
      new String(result.slice(1, 4)) should be("PNG")
    }
  }

  it should "do multiple operations" in {
    implicit val clientConfig: ClientConfig = ClientConfig()
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
