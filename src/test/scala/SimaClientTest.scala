import api.ImageInfo
import cats.effect.testing.scalatest.AsyncIOSpec
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should

import java.nio.file.{Files, Path}

class SimaClientTest extends AsyncFlatSpec with AsyncIOSpec with should.Matchers {
  behavior of "SimaClient"

  it should "request service status" in {
    implicit val clientConfig: ClientConfig = ClientConfig()
    SimaClient.client.health.asserting { result =>
      result should be(true)
    }
  }

  it should "request image info" in {
    val image = getClass.getResourceAsStream("test.webp").readAllBytes()
    println(image.size)
    implicit val clientConfig: ClientConfig = ClientConfig()
    SimaClient.client.info(image).asserting { result =>
      result should be(ImageInfo(100, 100, "webp", "srgb", hasAlpha = false, hasProfile = false, 3, 1))
    }
  }

  it should "request image resize" in {
    val image = getClass.getResourceAsStream("test.webp").readAllBytes()
    println(image.size)
    implicit val clientConfig: ClientConfig = ClientConfig()
    SimaClient.client.resize(image)(20).asserting { result =>
      Files.write(Path.of("out.webp"), result)
      result.length should be(394)
    }
  }
}
