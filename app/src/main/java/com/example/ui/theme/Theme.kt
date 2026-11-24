package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

private val AnimePlayColorScheme = darkColorScheme(
  primary = VlcOrange,
  onPrimary = OnPrimaryBlack,
  primaryContainer = VlcOrangeSoft,
  onPrimaryContainer = VlcOrange,
  secondary = SecondaryGray,
  onSecondary = OnPrimaryBlack,
  background = ObsidianBackground,
  onBackground = TextPrimary,
  surface = ObsidianSurface,
  onSurface = TextPrimary,
  surfaceVariant = ObsidianSurfaceElevated,
  onSurfaceVariant = SecondaryGray,
  outline = ObsidianBorder,
  error = CrimsonError,
  onError = TextPrimary
)

private val AnimePlayShapes = Shapes(
  small = RoundedCornerShape(8.dp),
  medium = RoundedCornerShape(14.dp),
  large = RoundedCornerShape(20.dp)
)

@Composable
fun AnimePlayTheme(
  content: @Composable () -> Unit
) {
  MaterialTheme(
    colorScheme = AnimePlayColorScheme,
    typography = AnimePlayTypography,
    shapes = AnimePlayShapes,
    content = content
  )
}
