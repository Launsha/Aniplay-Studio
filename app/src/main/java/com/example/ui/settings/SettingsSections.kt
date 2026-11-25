package com.example.ui.settings

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.preferences.AppSettings
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceElevated
import com.example.ui.theme.OnPrimaryBlack
import com.example.ui.theme.SecondaryGray
import com.example.ui.theme.VlcOrange
import kotlin.math.roundToInt

@Composable
fun SettingsSectionCard(
  title: String,
  content: @Composable () -> Unit
) {
  Surface(
    color = ObsidianSurface,
    shape = RoundedCornerShape(18.dp),
    modifier = Modifier
      .fillMaxWidth()
      .border(1.dp, ObsidianBorder, RoundedCornerShape(18.dp))
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = VlcOrange
      )
      content()
    }
  }
}

@Composable
fun SettingsToggleRow(
  title: String,
  subtitle: String,
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
      Text(
        text = title,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface
      )
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = SecondaryGray
      )
    }
    Switch(
      checked = checked,
      onCheckedChange = onCheckedChange,
      colors = SwitchDefaults.colors(
        checkedThumbColor = OnPrimaryBlack,
        checkedTrackColor = VlcOrange,
        uncheckedThumbColor = SecondaryGray,
        uncheckedTrackColor = ObsidianSurfaceElevated
      )
    )
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun <T> SettingsChipGroup(
  title: String,
  options: List<Pair<T, String>>,
  selectedValue: T,
  onSelect: (T) -> Unit
) {
  Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
    Text(
      text = title,
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurface
    )
    FlowRow(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      options.forEach { (value, label) ->
        FilterChip(
          selected = selectedValue == value,
          onClick = { onSelect(value) },
          label = { Text(text = label, style = MaterialTheme.typography.labelSmall) },
          colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = VlcOrange,
            selectedLabelColor = OnPrimaryBlack,
            containerColor = ObsidianSurfaceElevated,
            labelColor = SecondaryGray
          )
        )
      }
    }
  }
}

@Composable
fun VlcVideoSettingsSection(
  settings: AppSettings,
  viewModel: SettingsViewModel
) {
  SettingsSectionCard(title = stringResource(R.string.settings_video_vlc_section)) {
    SettingsChipGroup(
      title = stringResource(R.string.settings_hw_accel),
      options = listOf(
        "auto" to stringResource(R.string.settings_hw_auto),
        "hardware" to stringResource(R.string.settings_hw_hardware),
        "software" to stringResource(R.string.settings_hw_software)
      ),
      selectedValue = settings.hardwareAccelerationMode,
      onSelect = viewModel::setHardwareAccelerationMode
    )

    HorizontalDivider(color = ObsidianBorder)

    SettingsChipGroup(
      title = stringResource(R.string.settings_aspect_ratio),
      options = listOf(
        "fit" to stringResource(R.string.settings_aspect_fit),
        "fill" to stringResource(R.string.settings_aspect_fill),
        "zoom" to stringResource(R.string.settings_aspect_zoom),
        "16_9" to stringResource(R.string.settings_aspect_16_9),
        "4_3" to stringResource(R.string.settings_aspect_4_3)
      ),
      selectedValue = settings.defaultAspectRatio,
      onSelect = viewModel::setDefaultAspectRatio
    )

    HorizontalDivider(color = ObsidianBorder)

    SettingsChipGroup(
      title = stringResource(R.string.settings_orientation),
      options = listOf(
        "sensor_landscape" to stringResource(R.string.settings_orient_sensor),
        "locked_landscape" to stringResource(R.string.settings_orient_locked),
        "system" to stringResource(R.string.settings_orient_system)
      ),
      selectedValue = settings.playerOrientationMode,
      onSelect = viewModel::setPlayerOrientationMode
    )

    HorizontalDivider(color = ObsidianBorder)

    SettingsToggleRow(
      title = stringResource(R.string.settings_pip_title),
      subtitle = stringResource(R.string.settings_pip_desc),
      checked = settings.enablePictureInPicture,
      onCheckedChange = viewModel::setEnablePictureInPicture
    )

    HorizontalDivider(color = ObsidianBorder)

    SettingsToggleRow(
      title = stringResource(R.string.settings_keep_screen_on),
      subtitle = stringResource(R.string.settings_keep_screen_on_desc),
      checked = settings.keepScreenOn,
      onCheckedChange = viewModel::setKeepScreenOn
    )

    HorizontalDivider(color = ObsidianBorder)

    SettingsToggleRow(
      title = stringResource(R.string.settings_pinch_zoom),
      subtitle = stringResource(R.string.settings_pinch_zoom_desc),
      checked = settings.enablePinchZoom,
      onCheckedChange = viewModel::setEnablePinchZoom
    )

    HorizontalDivider(color = ObsidianBorder)

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
      Text(
        text = stringResource(R.string.player_night_dimmer_title, settings.nightDimmerPercent),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface
      )
      Slider(
        value = settings.nightDimmerPercent.toFloat(),
        onValueChange = { viewModel.setNightDimmerPercent(it.roundToInt()) },
        valueRange = 0f..60f,
        steps = 5,
        colors = SliderDefaults.colors(
          thumbColor = VlcOrange,
          activeTrackColor = VlcOrange,
          inactiveTrackColor = ObsidianSurfaceElevated
        )
      )
    }
  }
}

@Composable
fun VlcAudioSettingsSection(
  settings: AppSettings,
  viewModel: SettingsViewModel
) {
  SettingsSectionCard(title = stringResource(R.string.settings_audio_vlc_section)) {
    SettingsChipGroup(
      title = stringResource(R.string.settings_eq_preset),
      options = listOf(
        "normal" to stringResource(R.string.settings_eq_normal),
        "anime" to stringResource(R.string.settings_eq_anime),
        "bass" to stringResource(R.string.settings_eq_bass),
        "cinema" to stringResource(R.string.settings_eq_cinema),
        "night" to stringResource(R.string.settings_eq_night)
      ),
      selectedValue = settings.audioEqualizerPreset,
      onSelect = viewModel::setAudioEqualizerPreset
    )

    HorizontalDivider(color = ObsidianBorder)

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
      Text(
        text = stringResource(R.string.settings_audio_boost, settings.audioBoostPercent),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface
      )
      Slider(
        value = settings.audioBoostPercent.toFloat(),
        onValueChange = { viewModel.setAudioBoostPercent(it.roundToInt()) },
        valueRange = 100f..200f,
        steps = 9,
        colors = SliderDefaults.colors(
          thumbColor = VlcOrange,
          activeTrackColor = VlcOrange,
          inactiveTrackColor = ObsidianSurfaceElevated
        )
      )
    }

    HorizontalDivider(color = ObsidianBorder)

    SettingsToggleRow(
      title = stringResource(R.string.settings_skip_silence),
      subtitle = stringResource(R.string.settings_skip_silence_desc),
      checked = settings.skipSilence,
      onCheckedChange = viewModel::setSkipSilence
    )

    HorizontalDivider(color = ObsidianBorder)

    SettingsToggleRow(
      title = stringResource(R.string.settings_background_audio),
      subtitle = stringResource(R.string.settings_background_audio_desc),
      checked = settings.backgroundAudioPlay,
      onCheckedChange = viewModel::setBackgroundAudioPlay
    )

    HorizontalDivider(color = ObsidianBorder)

    SettingsChipGroup(
      title = stringResource(R.string.settings_preferred_audio),
      options = listOf(
        "auto" to stringResource(R.string.settings_audio_lang_auto),
        "jpn" to stringResource(R.string.settings_audio_lang_jpn),
        "eng" to stringResource(R.string.settings_audio_lang_eng),
        "ind" to stringResource(R.string.settings_audio_lang_ind)
      ),
      selectedValue = settings.preferredAudioLanguage,
      onSelect = viewModel::setPreferredAudioLanguage
    )
  }
}

@Composable
fun VlcSubtitleSettingsSection(
  settings: AppSettings,
  viewModel: SettingsViewModel
) {
  SettingsSectionCard(title = stringResource(R.string.settings_subtitle_vlc_section)) {
    SettingsToggleRow(
      title = stringResource(R.string.settings_auto_subtitle),
      subtitle = stringResource(R.string.settings_auto_subtitle_desc),
      checked = settings.autoEnableSubtitles,
      onCheckedChange = viewModel::setAutoEnableSubtitles
    )

    HorizontalDivider(color = ObsidianBorder)

    SettingsChipGroup(
      title = stringResource(R.string.settings_subtitle_size),
      options = listOf(
        14 to stringResource(R.string.settings_subtitle_size_small),
        18 to stringResource(R.string.settings_subtitle_size_normal),
        22 to stringResource(R.string.settings_subtitle_size_large),
        26 to stringResource(R.string.settings_subtitle_size_xlarge)
      ),
      selectedValue = settings.subtitleFontSizeSp,
      onSelect = viewModel::setSubtitleFontSizeSp
    )

    HorizontalDivider(color = ObsidianBorder)

    SettingsChipGroup(
      title = stringResource(R.string.settings_subtitle_color),
      options = listOf(
        "white" to stringResource(R.string.settings_sub_color_white),
        "yellow" to stringResource(R.string.settings_sub_color_yellow),
        "orange" to stringResource(R.string.settings_sub_color_orange),
        "cyan" to stringResource(R.string.settings_sub_color_cyan)
      ),
      selectedValue = settings.subtitleColorPreset,
      onSelect = viewModel::setSubtitleColorPreset
    )

    HorizontalDivider(color = ObsidianBorder)

    SettingsToggleRow(
      title = stringResource(R.string.settings_subtitle_bg),
      subtitle = stringResource(R.string.settings_subtitle_bg_desc),
      checked = settings.subtitleBackgroundBox,
      onCheckedChange = viewModel::setSubtitleBackgroundBox
    )

    HorizontalDivider(color = ObsidianBorder)

    val delayPrefix = if (settings.subtitleDelayMs > 0) "+" else ""
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
      Text(
        text = stringResource(R.string.player_sub_delay_title, "$delayPrefix${settings.subtitleDelayMs}"),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface
      )
      Slider(
        value = settings.subtitleDelayMs.toFloat(),
        onValueChange = { viewModel.setSubtitleDelayMs((it / 100f).roundToInt() * 100) },
        valueRange = -5000f..5000f,
        colors = SliderDefaults.colors(
          thumbColor = VlcOrange,
          activeTrackColor = VlcOrange,
          inactiveTrackColor = ObsidianSurfaceElevated
        )
      )
    }
  }
}
