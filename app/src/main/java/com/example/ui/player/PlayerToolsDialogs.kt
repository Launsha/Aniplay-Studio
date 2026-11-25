package com.example.ui.player

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Audiotrack
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PictureInPictureAlt
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.media3.common.TrackGroup
import com.example.R
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceElevated
import com.example.ui.theme.OnPrimaryBlack
import com.example.ui.theme.SecondaryGray
import com.example.ui.theme.VlcOrange
import kotlin.math.roundToInt

data class PlayerTrackOption(
  val mediaTrackGroup: TrackGroup,
  val trackIndex: Int,
  val label: String,
  val isSelected: Boolean
)

@Composable
fun TrackSelectorDialog(
  audioTracks: List<PlayerTrackOption>,
  subtitleTracks: List<PlayerTrackOption>,
  onSelectAudioTrack: (PlayerTrackOption) -> Unit,
  onSelectSubtitleTrack: (PlayerTrackOption) -> Unit,
  onPickExternalSubtitle: () -> Unit,
  onDismiss: () -> Unit
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = ObsidianSurface,
    title = {
      Text(
        text = stringResource(R.string.player_tracks_title),
        style = MaterialTheme.typography.titleLarge
      )
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .heightIn(max = 380.dp)
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Icon(
            imageVector = Icons.Rounded.Audiotrack,
            contentDescription = null,
            tint = VlcOrange,
            modifier = Modifier.size(18.dp)
          )
          Text(
            text = stringResource(R.string.player_audio_tracks),
            style = MaterialTheme.typography.titleSmall,
            color = VlcOrange
          )
        }
        if (audioTracks.isEmpty()) {
          Text(
            text = stringResource(R.string.player_no_audio_tracks),
            style = MaterialTheme.typography.bodySmall,
            color = SecondaryGray
          )
        } else {
          audioTracks.forEach { track ->
            TrackOptionRow(
              label = track.label,
              isSelected = track.isSelected,
              onClick = {
                onSelectAudioTrack(track)
                onDismiss()
              }
            )
          }
        }

        HorizontalDivider(color = ObsidianBorder)

        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Icon(
            imageVector = Icons.Rounded.Subtitles,
            contentDescription = null,
            tint = VlcOrange,
            modifier = Modifier.size(18.dp)
          )
          Text(
            text = stringResource(R.string.player_subtitle_tracks),
            style = MaterialTheme.typography.titleSmall,
            color = VlcOrange
          )
        }
        subtitleTracks.forEach { track ->
          TrackOptionRow(
            label = track.label,
            isSelected = track.isSelected,
            onClick = {
              onSelectSubtitleTrack(track)
              onDismiss()
            }
          )
        }
        OutlinedButton(
          onClick = {
            onDismiss()
            onPickExternalSubtitle()
          },
          modifier = Modifier.fillMaxWidth()
        ) {
          Text(
            text = stringResource(R.string.player_select_subtitle),
            color = VlcOrange
          )
        }
      }
    },
    confirmButton = {
      TextButton(onClick = onDismiss) {
        Text(stringResource(R.string.action_close), color = SecondaryGray)
      }
    }
  )
}

@Composable
private fun TrackOptionRow(
  label: String,
  isSelected: Boolean,
  onClick: () -> Unit
) {
  Surface(
    color = ObsidianSurfaceElevated,
    shape = RoundedCornerShape(10.dp),
    modifier = Modifier
      .fillMaxWidth()
      .border(
        1.dp,
        if (isSelected) VlcOrange else ObsidianBorder,
        RoundedCornerShape(10.dp)
      )
      .clip(RoundedCornerShape(10.dp))
      .clickable(onClick = onClick)
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Text(
        text = label,
        style = MaterialTheme.typography.bodySmall,
        color = if (isSelected) VlcOrange else MaterialTheme.colorScheme.onSurface
      )
      if (isSelected) {
        Icon(
          imageVector = Icons.Rounded.Check,
          contentDescription = null,
          tint = VlcOrange,
          modifier = Modifier.size(16.dp)
        )
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VlcToolsDialog(
  eqPreset: String,
  audioBoostPercent: Int,
  sleepMinutesRemaining: Int?,
  sleepOnEpisodeEnd: Boolean,
  repeatMode: String,
  abPointAMs: Long?,
  abPointBMs: Long?,
  subtitleDelayMs: Int,
  nightDimmerPercent: Int,
  autoSkipIntro: Boolean,
  autoSkipOutro: Boolean,
  backgroundAudioPlay: Boolean,
  canUsePip: Boolean,
  onSelectEqPreset: (String) -> Unit,
  onChangeAudioBoost: (Int) -> Unit,
  onSelectSleepMinutes: (Int?) -> Unit,
  onSelectSleepEndOfEpisode: () -> Unit,
  onSelectRepeatMode: (String) -> Unit,
  onSetAbPointA: () -> Unit,
  onSetAbPointB: () -> Unit,
  onClearAbRepeat: () -> Unit,
  onChangeSubtitleDelay: (Int) -> Unit,
  onChangeNightDimmer: (Int) -> Unit,
  onToggleAutoSkipIntro: (Boolean) -> Unit,
  onToggleAutoSkipOutro: (Boolean) -> Unit,
  onToggleBackgroundAudio: (Boolean) -> Unit,
  onOpenIntroOutroScanner: () -> Unit,
  onOpenJumpToTime: () -> Unit,
  onOpenMediaInfo: () -> Unit,
  onCaptureScreenshot: () -> Unit,
  onAddBookmark: () -> Unit,
  onEnterPip: () -> Unit,
  onDismiss: () -> Unit
) {
  val chipColors = FilterChipDefaults.filterChipColors(
    selectedContainerColor = VlcOrange,
    selectedLabelColor = OnPrimaryBlack,
    containerColor = ObsidianSurfaceElevated,
    labelColor = SecondaryGray
  )

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = ObsidianSurface,
    title = {
      Text(
        text = stringResource(R.string.player_more_tools),
        style = MaterialTheme.typography.titleLarge
      )
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .heightIn(max = 450.dp)
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        // Intro & Outro Scanner + Auto-skip
        OutlinedButton(
          onClick = {
            onDismiss()
            onOpenIntroOutroScanner()
          },
          modifier = Modifier.fillMaxWidth()
        ) {
          Icon(
            imageVector = Icons.Rounded.AutoFixHigh,
            contentDescription = null,
            tint = VlcOrange,
            modifier = Modifier.size(18.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = stringResource(R.string.intro_outro_dialog_title),
            color = VlcOrange
          )
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          FilterChip(
            selected = autoSkipIntro,
            onClick = { onToggleAutoSkipIntro(!autoSkipIntro) },
            label = { Text(stringResource(R.string.settings_auto_skip_intro), style = MaterialTheme.typography.labelSmall) },
            colors = chipColors
          )
          FilterChip(
            selected = autoSkipOutro,
            onClick = { onToggleAutoSkipOutro(!autoSkipOutro) },
            label = { Text(stringResource(R.string.settings_auto_skip_outro), style = MaterialTheme.typography.labelSmall) },
            colors = chipColors
          )
          FilterChip(
            selected = backgroundAudioPlay,
            onClick = { onToggleBackgroundAudio(!backgroundAudioPlay) },
            label = { Text(stringResource(R.string.player_background_audio_toggle), style = MaterialTheme.typography.labelSmall) },
            colors = chipColors
          )
        }

        HorizontalDivider(color = ObsidianBorder)

        // A-B Repeat & Loop Mode
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Icon(Icons.Rounded.Repeat, contentDescription = null, tint = VlcOrange, modifier = Modifier.size(18.dp))
          Text(stringResource(R.string.player_ab_repeat_title), style = MaterialTheme.typography.titleSmall, color = VlcOrange)
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          FilterChip(
            selected = abPointAMs != null,
            onClick = onSetAbPointA,
            label = { Text(stringResource(R.string.player_ab_set_a), style = MaterialTheme.typography.labelSmall) },
            colors = chipColors
          )
          FilterChip(
            selected = abPointBMs != null,
            onClick = onSetAbPointB,
            label = { Text(stringResource(R.string.player_ab_set_b), style = MaterialTheme.typography.labelSmall) },
            colors = chipColors
          )
          if (abPointAMs != null || abPointBMs != null) {
            FilterChip(
              selected = false,
              onClick = onClearAbRepeat,
              label = { Text(stringResource(R.string.player_ab_clear), style = MaterialTheme.typography.labelSmall) },
              colors = chipColors
            )
          }
        }
        Text(stringResource(R.string.player_repeat_mode_title), style = MaterialTheme.typography.labelSmall, color = SecondaryGray)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          listOf(
            "off" to R.string.player_repeat_off,
            "one" to R.string.player_repeat_one,
            "all" to R.string.player_repeat_all
          ).forEach { (key, res) ->
            FilterChip(
              selected = repeatMode == key,
              onClick = { onSelectRepeatMode(key) },
              label = { Text(stringResource(res), style = MaterialTheme.typography.labelSmall) },
              colors = chipColors
            )
          }
        }

        HorizontalDivider(color = ObsidianBorder)

        // Equalizer & Boost
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Icon(
            imageVector = Icons.Rounded.Equalizer,
            contentDescription = null,
            tint = VlcOrange,
            modifier = Modifier.size(18.dp)
          )
          Text(
            text = stringResource(R.string.player_equalizer_title),
            style = MaterialTheme.typography.titleSmall,
            color = VlcOrange
          )
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          val presets = listOf(
            "normal" to R.string.settings_eq_normal,
            "anime" to R.string.settings_eq_anime,
            "bass" to R.string.settings_eq_bass,
            "cinema" to R.string.settings_eq_cinema,
            "night" to R.string.settings_eq_night
          )
          presets.forEach { (key, labelRes) ->
            FilterChip(
              selected = eqPreset == key,
              onClick = { onSelectEqPreset(key) },
              label = { Text(stringResource(labelRes), style = MaterialTheme.typography.labelSmall) },
              colors = chipColors
            )
          }
        }
        Text(
          text = stringResource(R.string.settings_audio_boost, audioBoostPercent),
          style = MaterialTheme.typography.labelSmall,
          color = SecondaryGray
        )
        Slider(
          value = audioBoostPercent.toFloat(),
          onValueChange = { onChangeAudioBoost(it.roundToInt()) },
          valueRange = 100f..200f,
          steps = 9,
          colors = SliderDefaults.colors(
            thumbColor = VlcOrange,
            activeTrackColor = VlcOrange,
            inactiveTrackColor = ObsidianSurfaceElevated
          )
        )

        HorizontalDivider(color = ObsidianBorder)

        // Subtitle Delay Sync & Night Dimmer
        val delayPrefix = if (subtitleDelayMs > 0) "+" else ""
        Text(
          text = stringResource(R.string.player_sub_delay_title, "$delayPrefix$subtitleDelayMs"),
          style = MaterialTheme.typography.labelSmall,
          color = SecondaryGray
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          listOf(-500, -100, 0, 100, 500).forEach { delta ->
            FilterChip(
              selected = delta == 0 && subtitleDelayMs == 0,
              onClick = {
                val next = if (delta == 0) 0 else subtitleDelayMs + delta
                onChangeSubtitleDelay(next)
              },
              label = {
                Text(
                  text = if (delta == 0) "0 ms" else if (delta > 0) "+${delta}ms" else "${delta}ms",
                  style = MaterialTheme.typography.labelSmall
                )
              },
              colors = chipColors
            )
          }
        }

        Text(
          text = stringResource(R.string.player_night_dimmer_title, nightDimmerPercent),
          style = MaterialTheme.typography.labelSmall,
          color = SecondaryGray
        )
        Slider(
          value = nightDimmerPercent.toFloat(),
          onValueChange = { onChangeNightDimmer(it.roundToInt()) },
          valueRange = 0f..60f,
          steps = 5,
          colors = SliderDefaults.colors(
            thumbColor = VlcOrange,
            activeTrackColor = VlcOrange,
            inactiveTrackColor = ObsidianSurfaceElevated
          )
        )

        HorizontalDivider(color = ObsidianBorder)

        // Sleep Timer
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Icon(
            imageVector = Icons.Rounded.Bedtime,
            contentDescription = null,
            tint = VlcOrange,
            modifier = Modifier.size(18.dp)
          )
          Text(
            text = stringResource(R.string.player_sleep_timer),
            style = MaterialTheme.typography.titleSmall,
            color = VlcOrange
          )
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          FilterChip(
            selected = sleepMinutesRemaining == null && !sleepOnEpisodeEnd,
            onClick = { onSelectSleepMinutes(null) },
            label = { Text(stringResource(R.string.player_sleep_off), style = MaterialTheme.typography.labelSmall) },
            colors = chipColors
          )
          listOf(15, 30, 45, 60).forEach { mins ->
            FilterChip(
              selected = sleepMinutesRemaining == mins,
              onClick = { onSelectSleepMinutes(mins) },
              label = {
                Text(
                  text = stringResource(R.string.player_sleep_minutes, mins),
                  style = MaterialTheme.typography.labelSmall
                )
              },
              colors = chipColors
            )
          }
          FilterChip(
            selected = sleepOnEpisodeEnd,
            onClick = onSelectSleepEndOfEpisode,
            label = { Text(stringResource(R.string.player_sleep_end_ep), style = MaterialTheme.typography.labelSmall) },
            colors = chipColors
          )
        }

        HorizontalDivider(color = ObsidianBorder)

        // Bookmark, Jump to Time, Screenshot, Media Info & PiP Actions
        OutlinedButton(
          onClick = {
            onAddBookmark()
            onDismiss()
          },
          modifier = Modifier.fillMaxWidth()
        ) {
          Icon(Icons.Rounded.BookmarkAdd, contentDescription = null, tint = VlcOrange, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text(stringResource(R.string.player_bookmark_moment), color = VlcOrange)
        }

        OutlinedButton(
          onClick = {
            onDismiss()
            onOpenJumpToTime()
          },
          modifier = Modifier.fillMaxWidth()
        ) {
          Icon(Icons.Rounded.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text(stringResource(R.string.player_jump_to_time), color = MaterialTheme.colorScheme.onSurface)
        }

        OutlinedButton(
          onClick = {
            onDismiss()
            onCaptureScreenshot()
          },
          modifier = Modifier.fillMaxWidth()
        ) {
          Icon(Icons.Rounded.CameraAlt, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text(stringResource(R.string.player_screenshot), color = MaterialTheme.colorScheme.onSurface)
        }

        OutlinedButton(
          onClick = {
            onDismiss()
            onOpenMediaInfo()
          },
          modifier = Modifier.fillMaxWidth()
        ) {
          Icon(Icons.Rounded.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text(stringResource(R.string.player_media_info), color = MaterialTheme.colorScheme.onSurface)
        }

        if (canUsePip) {
          OutlinedButton(
            onClick = {
              onDismiss()
              onEnterPip()
            },
            modifier = Modifier.fillMaxWidth()
          ) {
            Icon(
              imageVector = Icons.Rounded.PictureInPictureAlt,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onSurface,
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = stringResource(R.string.player_pip_mode),
              color = MaterialTheme.colorScheme.onSurface
            )
          }
        }
      }
    },
    confirmButton = {
      TextButton(onClick = onDismiss) {
        Text(stringResource(R.string.action_close), color = SecondaryGray)
      }
    }
  )
}
