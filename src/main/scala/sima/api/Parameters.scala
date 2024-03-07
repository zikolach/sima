package sima.api

import Parameters.{allowedParams, Endpoint}
import io.circe.Json
import io.circe.syntax.EncoderOps

case class Parameters(
  width: Option[Int] = None, // Width of image area to extract/resize
  height: Option[Int] = None, // Height of image area to extract/resize
  top: Option[Int] = None, // Top edge of area to extract. Example: 100
  left: Option[Int] = None, // Left edge of area to extract. Example: 100
  areawidth: Option[Int] = None, // Height area to extract. Example: 300
  areaheight: Option[Int] = None, // Width area to extract. Example: 300
  quality: Option[Int] = None, // JPEG image quality between 1-100. Defaults to 80
  compression: Option[Int] = None, // PNG compression level. Default: 6
  palette: Option[Boolean] = None, // Enable 8-bit quantisation. Works with only PNG images. Default: false
  rotate: Option[Int] = None, // Image rotation angle. Must be multiple of 90. Example: 180
  factor: Option[Int] = None, // Zoom factor level. Example: 2
  margin: Option[Int] = None, // Text area margin for watermark. Example: 50
  dpi: Option[Int] = None, // DPI value for watermark. Example: 150
  textwidth: Option[Int] = None, // Text area width for watermark. Example: 200
  opacity: Option[Float] = None, // Opacity level for watermark text or watermark image. Default: 0.2
  flip: Option[Boolean] = None, // Transform the resultant image with flip operation. Default: false
  flop: Option[Boolean] = None, // Transform the resultant image with flop operation. Default: false
  force: Option[Boolean] = None, // Force image transformation size. Default: false
  nocrop: Option[Boolean] = None, // Disable crop transformation. Defaults depend on the operation
  noreplicate: Option[Boolean] = None, // Disable text replication in watermark. Defaults to false
  norotation: Option[Boolean] = None, // Disable auto rotation based on EXIF orientation. Defaults to false
  noprofile: Option[Boolean] = None, // Disable adding ICC profile metadata. Defaults to false
  stripmeta: Option[Boolean] = None, // Remove original image metadata, such as EXIF metadata. Defaults to false
  text: Option[String] = None, // Watermark text content. Example: copyright (c) 2189
  font: Option[String] = None, // Watermark text font type and format. Example: sans bold 12
  color: Option[String] = None, // Watermark text RGB decimal base color. Example: 255,200,150
  image: Option[String] = None, // Watermark image URL pointing to the remote HTTP server.
  `type`: Option[String] = None, // Specify the image format to output. Possible values are: jpeg, png, webp and auto. auto will use the preferred format requested by the client in the HTTP Accept header. A client can provide multiple comma-separated choices in Accept with the best being the one picked.
  gravity: Option[String] = None, // Define the crop operation gravity. Supported values are: north, south, centre, west, east and smart. Defaults to centre.
  file: Option[String] = None, // Use image from server local file path. In order to use this you must pass the -mount=<dir> flag.
  url: Option[String] = None, // Fetch the image from a remote HTTP server. In order to use this you must pass the -enable-url-source flag.
  colorspace: Option[String] = None, // Use a custom color space for the output image. Allowed values are: srgb or bw (black&white)
  field: Option[String] = None, // Custom image form field name if using multipart/form. Defaults to: file
  extend: Option[String] = None, // Extend represents the image extend mode used when the edges of an image are extended. Defaults to mirror. Allowed values are: black, copy, mirror, white, lastpixel and background. If background value is specified, you can define the desired extend RGB color via background param, such as ?extend=background&background=250,20,10. For more info, see libvips docs.
  background: Option[String] = None, // Background RGB decimal base color to use when flattening transparent PNGs. Example: 255,200,150
  sigma: Option[Float] = None, // Size of the gaussian mask to use when blurring an image. Example: 15.0
  minampl: Option[Float] = None, // Minimum amplitude of the gaussian filter to use when blurring an image. Default: Example: 0.5
  operations: Option[Json] = None, // Pipeline of image operation transformations defined as URL safe encoded JSON array. See pipeline endpoints for more details.
  sign: Option[String] = None, // URL signature (URL-safe Base64-encoded HMAC digest)
  interlace: Option[Boolean] = None, // Use progressive / interlaced format of the image output. Defaults to false
  aspectratio: Option[String] = None // Apply aspect ratio by giving either image's height or width. Example: 16:9
) {

  def toQuery: Map[String, String] =
    Seq(
      "width"       -> width.map(_.toString),
      "height"      -> height.map(_.toString),
      "top"         -> top.map(_.toString),
      "left"        -> left.map(_.toString),
      "areawidth"   -> areawidth.map(_.toString),
      "areaheight"  -> areaheight.map(_.toString),
      "quality"     -> quality.map(_.toString),
      "compression" -> compression.map(_.toString),
      "palette"     -> palette.map(_.toString),
      "rotate"      -> rotate.map(_.toString),
      "factor"      -> factor.map(_.toString),
      "margin"      -> margin.map(_.toString),
      "dpi"         -> dpi.map(_.toString),
      "textwidth"   -> textwidth.map(_.toString),
      "opacity"     -> opacity.map(_.toString),
      "flip"        -> flip.map(_.toString),
      "flop"        -> flop.map(_.toString),
      "force"       -> force.map(_.toString),
      "nocrop"      -> nocrop.map(_.toString),
      "noreplicate" -> noreplicate.map(_.toString),
      "norotation"  -> norotation.map(_.toString),
      "noprofile"   -> noprofile.map(_.toString),
      "stripmeta"   -> stripmeta.map(_.toString),
      "text"        -> text,
      "font"        -> font,
      "color"       -> color,
      "image"       -> image,
      "type"        -> `type`,
      "gravity"     -> gravity,
      "file"        -> file,
      "url"         -> url,
      "colorspace"  -> colorspace,
      "field"       -> field,
      "extend"      -> extend,
      "background"  -> background,
      "sigma"       -> sigma.map(_.toString),
      "minampl"     -> minampl.map(_.toString),
      "operations"  -> operations.map(_.asJson.noSpaces),
      "sign"        -> sign,
      "interlace"   -> interlace.map(_.toString),
      "aspectratio" -> aspectratio,
    ).collect { case (key, Some(value)) => key -> value }.toMap

  def toQuery(endpoint: Endpoint): Map[String, String] = {
    val allowed = allowedParams.getOrElse(endpoint, Set.empty)
    toQuery.view.filterKeys(allowed.contains).toMap
  }
}

object Parameters {

  sealed trait Endpoint
  case object resize extends Endpoint
  case object convert extends Endpoint
  case object fit extends Endpoint

  val allowedParams: Map[Endpoint, Set[String]] = Map(
    fit -> Set(
      "width",
      "height",
      "quality",
      "compression",
      "type",
      "file",
      "url",
      "embed",
      "force",
      "rotate",
      "norotation",
      "noprofile",
      "stripmeta",
      "flip",
      "flop",
      "extend",
      "background",
      "colorspace",
      "sigma",
      "minampl",
      "field",
      "interlace",
      "aspectratio",
      "palette"
    ),
    resize -> Set(
      "width",
      "height",
      "quality",
      "compression",
      "type",
      "file",
      "url",
      "embed",
      "force",
      "rotate",
      "nocrop",
      "norotation",
      "noprofile",
      "stripmeta",
      "flip",
      "flop",
      "extend",
      "background",
      "colorspace",
      "sigma",
      "minampl",
      "field",
      "interlace",
      "aspectratio",
      "palette",
    ),
    convert -> Set(
      "type",
      "quality",
      "compression",
      "file",
      "url",
      "embed",
      "force",
      "rotate",
      "norotation",
      "noprofile",
      "stripmeta",
      "flip",
      "flop",
      "extend",
      "background",
      "colorspace",
      "sigma",
      "minampl",
      "field",
      "interlace",
      "aspectratio",
      "palette",
    )
  )

}
