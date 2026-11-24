package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.videoFramePercent
import com.example.R
import com.example.ui.theme.ObsidianSurfaceElevated

@Composable
fun VideoThumbnail(
  uri: String?,
  contentDescription: String,
  modifier: Modifier = Modifier,
  targetWidthPx: Int = 480,
  targetHeightPx: Int = 360
) {
  val context = LocalContext.current
  Box(
    modifier = modifier.background(ObsidianSurfaceElevated)
  ) {
    if (uri.isNullOrBlank()) {
      Image(
        painter = painterResource(id = R.drawable.img_anime_cover_fallback),
        contentDescription = contentDescription,
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize()
      )
    } else {
      AsyncImage(
        model = ImageRequest.Builder(context)
          .data(uri)
          .videoFramePercent(0.15)
          .size(targetWidthPx, targetHeightPx)
          .crossfade(true)
          .placeholder(R.drawable.img_anime_cover_fallback)
          .error(R.drawable.img_anime_cover_fallback)
          .fallback(R.drawable.img_anime_cover_fallback)
          .build(),
        contentDescription = contentDescription,
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize()
      )
    }
  }
}
