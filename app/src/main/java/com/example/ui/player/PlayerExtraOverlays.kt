package com.example.ui.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.ObsidianBackground
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.OnPrimaryBlack
import com.example.ui.theme.VlcOrange
import com.example.util.TimeFormatters
import com.example.util.scaleOnPress

@Composable
fun NightDimmerLayer(dimmerPercent: Int) {
  if (dimmerPercent > 0) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(Color.Black.copy(alpha = (dimmerPercent / 100f).coerceIn(0f, 0.65f)))
    )
  }
}

@Composable
fun HorizontalSwipeSeekOverlay(
  deltaMs: Long?,
  targetPositionMs: Long
) {
  if (deltaMs == null) return
  Box(modifier = Modifier.fillMaxSize()) {
    Surface(
      color = ObsidianBackground.copy(alpha = 0.88f),
      shape = RoundedCornerShape(16.dp),
      modifier = Modifier
        .align(Alignment.Center)
        .border(1.dp, VlcOrange.copy(alpha = 0.7f), RoundedCornerShape(16.dp))
        .testTag("horizontal_swipe_seek_overlay")
    ) {
      val formattedDelta = TimeFormatters.formatDuration(kotlin.math.abs(deltaMs))
      val formattedTarget = TimeFormatters.formatDuration(targetPositionMs)
      Text(
        text = stringResource(
          if (deltaMs >= 0L) R.string.player_swipe_seek_forward else R.string.player_swipe_seek_backward,
          formattedDelta,
          formattedTarget
        ),
        style = MaterialTheme.typography.titleMedium,
        color = VlcOrange,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
      )
    }
  }
}

@Composable
fun AbRepeatActiveBadge(
  pointAMs: Long?,
  pointBMs: Long?,
  onClear: () -> Unit
) {
  if (pointAMs == null || pointBMs == null) return
  Box(modifier = Modifier.fillMaxSize()) {
    Surface(
      color = ObsidianBackground.copy(alpha = 0.82f),
      shape = RoundedCornerShape(50),
      modifier = Modifier
        .align(Alignment.TopStart)
        .windowInsetsPadding(WindowInsets.safeDrawing)
        .padding(start = 16.dp, top = 62.dp)
        .border(1.dp, VlcOrange.copy(alpha = 0.65f), RoundedCornerShape(50))
        .clip(RoundedCornerShape(50))
        .clickable(onClick = onClear)
        .testTag("ab_repeat_badge")
    ) {
      Row(
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        Icon(
          imageVector = Icons.Rounded.Repeat,
          contentDescription = null,
          tint = VlcOrange,
          modifier = Modifier.size(16.dp)
        )
        Text(
          text = stringResource(
            R.string.player_ab_active_badge,
            TimeFormatters.formatDuration(pointAMs),
            TimeFormatters.formatDuration(pointBMs)
          ),
          style = MaterialTheme.typography.labelMedium,
          color = VlcOrange
        )
        Icon(
          imageVector = Icons.Rounded.Close,
          contentDescription = stringResource(R.string.player_ab_clear),
          tint = VlcOrange,
          modifier = Modifier.size(15.dp)
        )
      }
    }
  }
}

@Composable
fun FloatingSkipIntroOutroPills(
  inIntro: Boolean,
  inOutro: Boolean,
  hasNextEpisode: Boolean,
  controlsVisible: Boolean,
  isScreenLocked: Boolean,
  onSkipIntro: () -> Unit,
  onSkipOutro: () -> Unit
) {
  val bottomOffset = if (controlsVisible) 104.dp else 24.dp
  Box(modifier = Modifier.fillMaxSize()) {
    AnimatedVisibility(
      visible = !isScreenLocked && (inIntro || inOutro),
      enter = fadeIn(),
      exit = fadeOut(),
      modifier = Modifier
        .align(Alignment.BottomEnd)
        .windowInsetsPadding(WindowInsets.safeDrawing)
        .padding(end = 20.dp, bottom = bottomOffset)
    ) {
      val interaction = remember { MutableInteractionSource() }
      Button(
        onClick = if (inIntro) onSkipIntro else onSkipOutro,
        interactionSource = interaction,
        colors = ButtonDefaults.buttonColors(
          containerColor = ObsidianSurface.copy(alpha = 0.94f),
          contentColor = VlcOrange
        ),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
          .height(48.dp)
          .scaleOnPress(interaction)
          .border(1.5.dp, VlcOrange, RoundedCornerShape(14.dp))
          .testTag(if (inIntro) "floating_skip_intro_button" else "floating_skip_outro_button")
      ) {
        Icon(
          imageVector = if (inIntro || !hasNextEpisode) Icons.Rounded.FastForward else Icons.Rounded.SkipNext,
          contentDescription = null,
          tint = VlcOrange,
          modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = when {
            inIntro -> stringResource(R.string.player_skip_intro)
            hasNextEpisode -> stringResource(R.string.player_skip_outro_next)
            else -> stringResource(R.string.player_skip_outro)
          },
          style = MaterialTheme.typography.labelLarge,
          color = OnPrimaryBlack.takeIf { false } ?: VlcOrange
        )
      }
    }
  }
}
