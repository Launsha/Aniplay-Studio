package com.example.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

object AnimePlayRoleShapes {
  val Poster = RoundedCornerShape(6.dp)
  val Button = CircleShape
  val BottomSheet = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
  val Dialog = RoundedCornerShape(16.dp)
  val Chip = CircleShape
  val Input = RoundedCornerShape(12.dp)
}

private val AnimePlayColorScheme = darkColorScheme(
  primary = VlcOrange,
  onPrimary = OnPrimaryBlack,
  primaryContainer = ObsidianSurfaceElevated,
  onPrimaryContainer = TextPrimary,
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
  small = RoundedCornerShape(6.dp),
  medium = RoundedCornerShape(12.dp),
  large = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
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
