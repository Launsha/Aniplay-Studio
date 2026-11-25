package com.example.ui.player

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.entity.EpisodeEntity
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceElevated
import com.example.ui.theme.OnPrimaryBlack
import com.example.ui.theme.SecondaryGray
import com.example.ui.theme.VlcOrange
import com.example.util.TimeFormatters

@Composable
fun IntroOutroScannerDialog(
  episode: EpisodeEntity?,
  currentPositionMs: Long,
  isScanning: Boolean,
  scanMessage: String?,
  onRunAutoScan: () -> Unit,
  onSaveSegments: (
    introStartMs: Long,
    introEndMs: Long,
    outroStartMs: Long,
    outroEndMs: Long,
    source: String,
    applyToAll: Boolean
  ) -> Unit,
  onDismiss: () -> Unit
) {
  var introStart by remember(episode?.id, episode?.introStartMs) {
    mutableLongStateOf(episode?.introStartMs ?: -1L)
  }
  var introEnd by remember(episode?.id, episode?.introEndMs) {
    mutableLongStateOf(episode?.introEndMs ?: -1L)
  }
  var outroStart by remember(episode?.id, episode?.outroStartMs) {
    mutableLongStateOf(episode?.outroStartMs ?: -1L)
  }
  var outroEnd by remember(episode?.id, episode?.outroEndMs) {
    mutableLongStateOf(episode?.outroEndMs ?: -1L)
  }
  var applyToAll by remember { mutableStateOf(false) }

  val sourceLabel = when (episode?.introOutroSource) {
    "chapter" -> stringResource(R.string.intro_outro_source_chapter)
    "subtitle" -> stringResource(R.string.intro_outro_source_subtitle)
    "audio_scan" -> stringResource(R.string.intro_outro_source_audio)
    "manual" -> stringResource(R.string.intro_outro_source_manual)
    else -> stringResource(R.string.intro_outro_source_none)
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = ObsidianSurface,
    title = {
      Text(
        text = stringResource(R.string.intro_outro_dialog_title),
        style = MaterialTheme.typography.titleLarge
      )
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .heightIn(max = 440.dp)
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Text(
          text = sourceLabel,
          style = MaterialTheme.typography.labelMedium,
          color = VlcOrange
        )

        Button(
          onClick = onRunAutoScan,
          enabled = !isScanning,
          colors = ButtonDefaults.buttonColors(
            containerColor = VlcOrange,
            contentColor = OnPrimaryBlack
          ),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("dialog_run_intro_scan_button")
        ) {
          if (isScanning) {
            CircularProgressIndicator(
              color = OnPrimaryBlack,
              strokeWidth = 2.dp,
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.intro_outro_scanning), style = MaterialTheme.typography.labelMedium)
          } else {
            Icon(Icons.Rounded.AutoFixHigh, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.intro_outro_scan_auto), style = MaterialTheme.typography.labelMedium)
          }
        }

        if (!scanMessage.isNullOrBlank()) {
          Text(
            text = scanMessage,
            style = MaterialTheme.typography.bodySmall,
            color = VlcOrange
          )
        }

        HorizontalDivider(color = ObsidianBorder)

        Text(
          text = stringResource(R.string.intro_section_label),
          style = MaterialTheme.typography.titleSmall,
          color = MaterialTheme.colorScheme.onSurface
        )
        Text(
          text = if (introStart >= 0L && introEnd > introStart) {
            stringResource(
              R.string.intro_outro_range_value,
              TimeFormatters.formatDuration(introStart),
              TimeFormatters.formatDuration(introEnd)
            )
          } else {
            stringResource(R.string.intro_outro_not_set)
          },
          style = MaterialTheme.typography.bodySmall,
          color = SecondaryGray
        )
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          OutlinedButton(
            onClick = {
              introStart = currentPositionMs
              if (introEnd <= introStart) introEnd = introStart + 85_000L
            },
            modifier = Modifier.weight(1f)
          ) {
            Text(stringResource(R.string.intro_mark_start), style = MaterialTheme.typography.labelSmall, color = VlcOrange)
          }
          OutlinedButton(
            onClick = {
              if (introStart < 0L) introStart = (currentPositionMs - 85_000L).coerceAtLeast(0L)
              introEnd = currentPositionMs.coerceAtLeast(introStart + 5_000L)
            },
            modifier = Modifier.weight(1f)
          ) {
            Text(stringResource(R.string.intro_mark_end), style = MaterialTheme.typography.labelSmall, color = VlcOrange)
          }
        }

        HorizontalDivider(color = ObsidianBorder)

        Text(
          text = stringResource(R.string.outro_section_label),
          style = MaterialTheme.typography.titleSmall,
          color = MaterialTheme.colorScheme.onSurface
        )
        Text(
          text = if (outroStart >= 0L && outroEnd > outroStart) {
            stringResource(
              R.string.intro_outro_range_value,
              TimeFormatters.formatDuration(outroStart),
              TimeFormatters.formatDuration(outroEnd)
            )
          } else {
            stringResource(R.string.intro_outro_not_set)
          },
          style = MaterialTheme.typography.bodySmall,
          color = SecondaryGray
        )
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          OutlinedButton(
            onClick = {
              outroStart = currentPositionMs
              val maxDur = episode?.durationMs?.takeIf { it > currentPositionMs } ?: (currentPositionMs + 90_000L)
              if (outroEnd <= outroStart) outroEnd = maxDur
            },
            modifier = Modifier.weight(1f)
          ) {
            Text(stringResource(R.string.outro_mark_start), style = MaterialTheme.typography.labelSmall, color = VlcOrange)
          }
          OutlinedButton(
            onClick = {
              if (outroStart < 0L) outroStart = (currentPositionMs - 85_000L).coerceAtLeast(0L)
              outroEnd = currentPositionMs.coerceAtLeast(outroStart + 5_000L)
            },
            modifier = Modifier.weight(1f)
          ) {
            Text(stringResource(R.string.outro_mark_end), style = MaterialTheme.typography.labelSmall, color = VlcOrange)
          }
        }

        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clickable { applyToAll = !applyToAll },
          verticalAlignment = Alignment.CenterVertically
        ) {
          Checkbox(
            checked = applyToAll,
            onCheckedChange = { applyToAll = it },
            colors = CheckboxDefaults.colors(checkedColor = VlcOrange, checkmarkColor = OnPrimaryBlack)
          )
          Text(
            text = stringResource(R.string.intro_outro_apply_all),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
          )
        }

        TextButton(
          onClick = {
            introStart = -1L
            introEnd = -1L
            outroStart = -1L
            outroEnd = -1L
            onSaveSegments(-1L, -1L, -1L, -1L, "", applyToAll)
          }
        ) {
          Text(stringResource(R.string.intro_outro_reset), color = MaterialTheme.colorScheme.error)
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          onSaveSegments(introStart, introEnd, outroStart, outroEnd, "manual", applyToAll)
          onDismiss()
        },
        colors = ButtonDefaults.buttonColors(containerColor = VlcOrange, contentColor = OnPrimaryBlack)
      ) {
        Text(stringResource(R.string.action_save))
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text(stringResource(R.string.action_close), color = SecondaryGray)
      }
    }
  )
}

@Composable
fun JumpToTimeDialog(
  currentPositionMs: Long,
  totalDurationMs: Long,
  onJump: (Long) -> Unit,
  onDismiss: () -> Unit
) {
  val curMin = (currentPositionMs / 60_000L).toInt()
  val curSec = ((currentPositionMs / 1000L) % 60L).toInt()
  var minText by remember { mutableStateOf(curMin.toString()) }
  var secText by remember { mutableStateOf(curSec.toString()) }

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = ObsidianSurface,
    icon = { Icon(Icons.Rounded.Schedule, contentDescription = null, tint = VlcOrange) },
    title = { Text(stringResource(R.string.player_jump_to_time), style = MaterialTheme.typography.titleLarge) },
    text = {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        OutlinedTextField(
          value = minText,
          onValueChange = { minText = it.filter { ch -> ch.isDigit() }.take(3) },
          label = { Text(stringResource(R.string.player_jump_minutes)) },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          singleLine = true,
          modifier = Modifier.weight(1f)
        )
        OutlinedTextField(
          value = secText,
          onValueChange = { secText = it.filter { ch -> ch.isDigit() }.take(2) },
          label = { Text(stringResource(R.string.player_jump_seconds)) },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          singleLine = true,
          modifier = Modifier.weight(1f)
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val m = minText.toLongOrNull() ?: 0L
          val s = secText.toLongOrNull() ?: 0L
          val targetMs = ((m * 60L) + s) * 1000L
          val maxDur = totalDurationMs.takeIf { it > 0L } ?: Long.MAX_VALUE
          onJump(targetMs.coerceIn(0L, maxDur))
          onDismiss()
        },
        colors = ButtonDefaults.buttonColors(containerColor = VlcOrange, contentColor = OnPrimaryBlack)
      ) {
        Text(stringResource(R.string.player_jump_go))
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text(stringResource(R.string.action_cancel), color = SecondaryGray)
      }
    }
  )
}

@Composable
fun MediaInfoDialog(
  fileName: String,
  resolution: String,
  videoCodec: String,
  audioCodec: String,
  durationMs: Long,
  onDismiss: () -> Unit
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = ObsidianSurface,
    icon = { Icon(Icons.Rounded.Info, contentDescription = null, tint = VlcOrange) },
    title = { Text(stringResource(R.string.player_media_info), style = MaterialTheme.typography.titleLarge) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        MediaInfoRow(stringResource(R.string.player_media_filename), fileName)
        MediaInfoRow(stringResource(R.string.player_media_resolution), resolution)
        MediaInfoRow(stringResource(R.string.player_media_video_codec), videoCodec)
        MediaInfoRow(stringResource(R.string.player_media_audio_codec), audioCodec)
        MediaInfoRow(
          stringResource(R.string.player_media_duration),
          TimeFormatters.formatDuration(durationMs)
        )
      }
    },
    confirmButton = {
      TextButton(onClick = onDismiss) {
        Text(stringResource(R.string.action_close), color = VlcOrange)
      }
    }
  )
}

@Composable
private fun MediaInfoRow(label: String, value: String) {
  Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
    Text(text = label, style = MaterialTheme.typography.labelSmall, color = VlcOrange)
    Text(
      text = value,
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurface
    )
  }
}

@Composable
fun PlayerEpisodeQueueDialog(
  episodes: List<EpisodeEntity>,
  currentEpisodeId: Long,
  onSelectEpisode: (EpisodeEntity) -> Unit,
  onDismiss: () -> Unit
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = ObsidianSurface,
    title = { Text(stringResource(R.string.player_queue_title), style = MaterialTheme.typography.titleLarge) },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .heightIn(max = 360.dp)
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        episodes.forEach { ep ->
          val isCurrent = ep.id == currentEpisodeId
          Surface(
            color = ObsidianSurfaceElevated,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
              .fillMaxWidth()
              .border(
                1.dp,
                if (isCurrent) VlcOrange else ObsidianBorder,
                RoundedCornerShape(10.dp)
              )
              .clip(RoundedCornerShape(10.dp))
              .clickable {
                onSelectEpisode(ep)
                onDismiss()
              }
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              Icon(
                imageVector = if (ep.isWatched) Icons.Rounded.CheckCircle else Icons.Rounded.PlayArrow,
                contentDescription = null,
                tint = if (isCurrent || ep.isWatched) VlcOrange else SecondaryGray,
                modifier = Modifier.size(18.dp)
              )
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = stringResource(R.string.detail_episode_label, ep.number),
                  style = MaterialTheme.typography.labelLarge,
                  color = if (isCurrent) VlcOrange else MaterialTheme.colorScheme.onSurface
                )
                Text(
                  text = ep.fileName,
                  style = MaterialTheme.typography.bodySmall,
                  color = SecondaryGray,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
              }
              if (ep.durationMs > 0L) {
                Text(
                  text = TimeFormatters.formatDuration(ep.durationMs),
                  style = MaterialTheme.typography.labelSmall,
                  color = SecondaryGray
                )
              }
            }
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
