package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.ColorUtils
import androidx.palette.graphics.Palette
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import coil.request.videoFramePercent
import com.example.ui.theme.ObsidianSurface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun rememberClampedDominantColor(
  imageUri: String?,
  fallbackColor: Color = ObsidianSurface
): Color {
  val context = LocalContext.current
  var tintedColor by remember(imageUri) { mutableStateOf(fallbackColor) }

  LaunchedEffect(imageUri) {
    if (imageUri.isNullOrBlank()) {
      tintedColor = fallbackColor
      return@LaunchedEffect
    }
    val extracted = withContext(Dispatchers.IO) {
      runCatching {
        val isVideoSource = !imageUri.startsWith("http", ignoreCase = true) &&
          !imageUri.contains("/covers/", ignoreCase = true) &&
          !imageUri.endsWith(".jpg", ignoreCase = true) &&
          !imageUri.endsWith(".jpeg", ignoreCase = true) &&
          !imageUri.endsWith(".png", ignoreCase = true) &&
          !imageUri.endsWith(".webp", ignoreCase = true)

        val request = ImageRequest.Builder(context)
          .data(imageUri)
          .apply {
            if (isVideoSource) videoFramePercent(0.15)
          }
          .size(160, 220)
          .allowHardware(false)
          .build()

        val result = context.imageLoader.execute(request) as? SuccessResult
        val bitmap = (result?.drawable as? BitmapDrawable)?.bitmap
          ?: return@runCatching fallbackColor
        extractClampedDarkTint(bitmap, fallbackColor)
      }.getOrDefault(fallbackColor)
    }
    tintedColor = extracted
  }

  return tintedColor
}

private fun extractClampedDarkTint(bitmap: Bitmap, fallbackColor: Color): Color {
  val palette = Palette.from(bitmap).maximumColorCount(12).generate()
  val swatchRgb = palette.dominantSwatch?.rgb
    ?: palette.darkVibrantSwatch?.rgb
    ?: palette.mutedSwatch?.rgb
    ?: return fallbackColor

  val hsl = FloatArray(3)
  ColorUtils.colorToHSL(swatchRgb, hsl)
  // Clamp saturation and lightness dark so header text always stays high-contrast
  hsl[1] = hsl[1].coerceIn(0.12f, 0.48f)
  hsl[2] = hsl[2].coerceIn(0.09f, 0.19f)
  val clampedArgb = ColorUtils.HSLToColor(hsl)
  // Blend slightly toward fallback dark surface to ensure smooth cohesion
  val blended = ColorUtils.blendARGB(clampedArgb, fallbackColor.toArgb(), 0.25f)
  return Color(blended)
}
