package demo.templates

import org.apache.pekko.http.scaladsl.model.{ContentTypes, HttpEntity}
import scalatags.Text.all._

object MainPage {
  def apply(): HttpEntity.Strict =
    HttpEntity(
      ContentTypes.`text/html(UTF-8)`,
      html(
        lang := "en",
        head(
          tag("title")("Sample form"),
        ),
        body(
          h1("Demo"),
          form(
            method := "POST",
            action := "/info",
            enctype := "multipart/form-data",
            input(`type` := "file", name := "file"),
            input(`type` := "submit", value := "Info")
          ),
          form(
            method := "POST",
            action := "/resize",
            enctype := "multipart/form-data",
            input(`type` := "file", name := "file"),
            input(`type` := "submit", value := "Convert and resize")
          ),
//          script(`type` := "text/javascript", src := "")
        )
      ).toString
    )
}
