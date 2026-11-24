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
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.PictureInPictureAlt
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
  canUsePip: Boolean,
  onSelectEqPreset: (String) -> Unit,
  onChangeAudioBoost: (Int) -> Unit,
  onSelectSleepMinutes: (Int?) -> Unit,
  onSelectSleepEndOfEpisode: () -> Unit,
  onAddBookmark: () -> Unit,
  onEnterPip: () -> Unit,
  onDismiss: () -> Unit
) {
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
          .heightIn(max = 420.dp)
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
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
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = VlcOrange,
                selectedLabelColor = OnPrimaryBlack,
                containerColor = ObsidianSurfaceElevated,
                labelColor = SecondaryGray
              )
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
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = VlcOrange,
              selectedLabelColor = OnPrimaryBlack,
              containerColor = ObsidianSurfaceElevated,
              labelColor = SecondaryGray
            )
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
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = VlcOrange,
                selectedLabelColor = OnPrimaryBlack,
                containerColor = ObsidianSurfaceElevated,
                labelColor = SecondaryGray
              )
            )
          }
          FilterChip(
            selected = sleepOnEpisodeEnd,
            onClick = onSelectSleepEndOfEpisode,
            label = { Text(stringResource(R.string.player_sleep_end_ep), style = MaterialTheme.typography.labelSmall) },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = VlcOrange,
              selectedLabelColor = OnPrimaryBlack,
              containerColor = ObsidianSurfaceElevated,
              labelColor = SecondaryGray
            )
          )
        }

        HorizontalDivider(color = ObsidianBorder)

        // Bookmark & PiP Actions
        OutlinedButton(
          onClick = {
            onAddBookmark()
            onDismiss()
          },
          modifier = Modifier.fillMaxWidth()
        ) {
          Icon(
            imageVector = Icons.Rounded.BookmarkAdd,
            contentDescription = null,
            tint = VlcOrange,
            modifier = Modifier.size(18.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = stringResource(R.string.player_bookmark_moment),
            color = VlcOrange
          )
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
