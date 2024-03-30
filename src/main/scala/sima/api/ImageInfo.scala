package sima.api

import io.circe.generic.JsonCodec

@JsonCodec
case class ImageInfo(
  width: Int,
  height: Int,
  `type`: String,
  space: String,
  hasAlpha: Boolean,
  hasProfile: Boolean,
  channels: Int,
  orientation: Int
)
