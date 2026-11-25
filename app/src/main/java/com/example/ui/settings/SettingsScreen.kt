package com.example.ui.settings

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.ui.theme.ObsidianBackground
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceElevated
import com.example.ui.theme.OnPrimaryBlack
import com.example.ui.theme.SecondaryGray
import com.example.ui.theme.VlcOrange
import com.example.util.scaleOnPress

private enum class SettingsCategory(val labelRes: Int) {
  GENERAL(R.string.settings_tab_general),
  VIDEO(R.string.settings_tab_video),
  AUDIO(R.string.settings_tab_audio),
  SUBTITLE(R.string.settings_tab_subtitle),
  CONTROLS(R.string.settings_tab_controls),
  ADVANCED(R.string.settings_tab_advanced)
}

private val speedChoices = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
private val thresholdChoices = listOf(85, 90, 95)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
  onBack: () -> Unit,
  onOpenGuide: () -> Unit = {},
  viewModel: SettingsViewModel = hiltViewModel()
) {
  Scaffold(
    containerColor = ObsidianBackground,
    contentWindowInsets = WindowInsets.statusBars,
    topBar = {
      TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = ObsidianBackground,
          titleContentColor = MaterialTheme.colorScheme.onBackground
        ),
        navigationIcon = {
          IconButton(
            onClick = onBack,
            modifier = Modifier
              .minimumInteractiveComponentSize()
              .testTag("settings_back_button")
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
              contentDescription = stringResource(R.string.action_back)
            )
          }
        },
        title = {
          Text(
            text = stringResource(R.string.settings_title),
            style = MaterialTheme.typography.titleLarge
          )
        }
      )
    }
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
      SettingsContent(onOpenGuide = onOpenGuide, viewModel = viewModel)
    }
  }
}

@Composable
fun SettingsContent(
  onOpenGuide: () -> Unit,
  viewModel: SettingsViewModel = hiltViewModel()
) {
  val context = LocalContext.current
  val state by viewModel.uiState.collectAsStateWithLifecycle()
  val snackbarHostState = remember { SnackbarHostState() }
  var showResetConfirm by remember { mutableStateOf(false) }
  var selectedCategory by rememberSaveable { mutableStateOf(SettingsCategory.GENERAL) }

  LaunchedEffect(state.statusMessage) {
    state.statusMessage?.let { msg ->
      snackbarHostState.showSnackbar(msg)
      viewModel.consumeMessage()
    }
  }

  val folderPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocumentTree()
  ) { uri ->
    if (uri != null) {
      runCatching {
        context.contentResolver.takePersistableUriPermission(
          uri,
          Intent.FLAG_GRANT_READ_URI_PERMISSION
        )
      }
      viewModel.addFolderFromTreeUri(uri.toString())
    }
  }

  val exportSuccessMsg = stringResource(R.string.settings_export_success)
  val malXmlLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.CreateDocument("text/xml")
  ) { uri ->
    if (uri != null) {
      viewModel.exportMalXml(uri, exportSuccessMsg)
    }
  }

  val cacheClearedMsg = stringResource(R.string.settings_cache_cleared)
  val statsResetMsg = stringResource(R.string.settings_stats_reset_done)
  val vlcResetMsg = stringResource(R.string.settings_reset_vlc_done)

  Box(modifier = Modifier.fillMaxSize()) {
    Column(modifier = Modifier.fillMaxSize()) {
      if (state.isScanning) {
        LinearProgressIndicator(
          color = VlcOrange,
          trackColor = ObsidianSurface,
          modifier = Modifier
            .fillMaxWidth()
            .height(3.dp)
        )
      }

      // VLC-style Horizontal Category Tabs
      LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.testTag("settings_category_tabs")
      ) {
        items(SettingsCategory.entries) { category ->
          FilterChip(
            selected = selectedCategory == category,
            onClick = { selectedCategory = category },
            label = {
              Text(
                text = stringResource(category.labelRes),
                style = MaterialTheme.typography.labelMedium
              )
            },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = VlcOrange,
              selectedLabelColor = OnPrimaryBlack,
              containerColor = ObsidianSurface,
              labelColor = SecondaryGray
            ),
            modifier = Modifier.testTag("settings_tab_${category.name.lowercase()}")
          )
        }
      }

      LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
          .fillMaxSize()
          .testTag("settings_list")
      ) {
        when (selectedCategory) {
          SettingsCategory.GENERAL -> {
            item {
              SettingsSectionCard(title = stringResource(R.string.settings_folders_section)) {
                Text(
                  text = stringResource(R.string.settings_folders_desc),
                  style = MaterialTheme.typography.bodySmall,
                  color = SecondaryGray
                )

                if (state.folders.isEmpty()) {
                  Text(
                    text = stringResource(R.string.settings_folder_empty),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                  )
                } else {
                  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.folders.forEach { folder ->
                      Surface(
                        color = ObsidianSurfaceElevated,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                          .fillMaxWidth()
                          .border(1.dp, ObsidianBorder, RoundedCornerShape(12.dp))
                      ) {
                        Row(
                          modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                          verticalAlignment = Alignment.CenterVertically,
                          horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                          Icon(
                            imageVector = Icons.Rounded.Folder,
                            contentDescription = null,
                            tint = VlcOrange,
                            modifier = Modifier.size(20.dp)
                          )
                          Text(
                            text = folder.displayLabel,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                          )
                          IconButton(
                            onClick = { viewModel.removeFolder(folder) },
                            modifier = Modifier.minimumInteractiveComponentSize()
                          ) {
                            Icon(
                              imageVector = Icons.Rounded.DeleteOutline,
                              contentDescription = stringResource(R.string.action_delete),
                              tint = SecondaryGray
                            )
                          }
                        }
                      }
                    }
                  }
                }

                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                  val addInteraction = remember { MutableInteractionSource() }
                  Button(
                    onClick = { folderPickerLauncher.launch(null) },
                    interactionSource = addInteraction,
                    colors = ButtonDefaults.buttonColors(
                      containerColor = VlcOrange,
                      contentColor = OnPrimaryBlack
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                      .weight(1f)
                      .height(48.dp)
                      .scaleOnPress(addInteraction)
                      .testTag("settings_add_folder_button")
                  ) {
                    Icon(
                      imageVector = Icons.Rounded.CreateNewFolder,
                      contentDescription = null,
                      modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                      text = stringResource(R.string.settings_add_folder),
                      style = MaterialTheme.typography.labelLarge,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis
                    )
                  }

                  val scanInteraction = remember { MutableInteractionSource() }
                  OutlinedButton(
                    onClick = { viewModel.scanNow() },
                    enabled = !state.isScanning,
                    interactionSource = scanInteraction,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                      .height(48.dp)
                      .scaleOnPress(scanInteraction)
                  ) {
                    Icon(
                      imageVector = Icons.Rounded.Refresh,
                      contentDescription = null,
                      tint = VlcOrange,
                      modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                      text = stringResource(R.string.settings_scan_now),
                      color = VlcOrange,
                      style = MaterialTheme.typography.labelLarge,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis
                    )
                  }
                }
              }
            }

            item {
              SettingsSectionCard(title = stringResource(R.string.settings_mal_section)) {
                SettingsToggleRow(
                  title = stringResource(R.string.settings_auto_enrich_mal),
                  subtitle = stringResource(R.string.settings_auto_enrich_mal_desc),
                  checked = state.settings.autoEnrichMal,
                  onCheckedChange = viewModel::setAutoEnrichMal
                )
                HorizontalDivider(color = ObsidianBorder)
                val exportInteraction = remember { MutableInteractionSource() }
                OutlinedButton(
                  onClick = { malXmlLauncher.launch("animeplay_myanimelist.xml") },
                  interactionSource = exportInteraction,
                  shape = RoundedCornerShape(12.dp),
                  modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .scaleOnPress(exportInteraction)
                    .testTag("settings_export_mal_button")
                ) {
                  Icon(
                    imageVector = Icons.Rounded.Download,
                    contentDescription = null,
                    tint = VlcOrange,
                    modifier = Modifier.size(18.dp)
                  )
                  Spacer(modifier = Modifier.width(8.dp))
                  Text(
                    text = stringResource(R.string.settings_export_mal_xml),
                    color = VlcOrange,
                    style = MaterialTheme.typography.labelLarge
                  )
                }
                Text(
                  text = stringResource(R.string.settings_export_mal_xml_desc),
                  style = MaterialTheme.typography.bodySmall,
                  color = SecondaryGray
                )
              }
            }

            item {
              SettingsSectionCard(title = stringResource(R.string.settings_storage_section)) {
                OutlinedButton(
                  onClick = onOpenGuide,
                  shape = RoundedCornerShape(12.dp),
                  modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                ) {
                  Icon(
                    imageVector = Icons.AutoMirrored.Rounded.HelpOutline,
                    contentDescription = null,
                    tint = VlcOrange,
                    modifier = Modifier.size(18.dp)
                  )
                  Spacer(modifier = Modifier.width(8.dp))
                  Text(
                    text = stringResource(R.string.settings_open_guide_button),
                    color = VlcOrange,
                    style = MaterialTheme.typography.labelLarge
                  )
                }

                OutlinedButton(
                  onClick = { viewModel.clearThumbnailCache(cacheClearedMsg) },
                  shape = RoundedCornerShape(12.dp),
                  modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                ) {
                  Text(
                    text = stringResource(R.string.settings_clear_cache),
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.labelLarge
                  )
                }

                OutlinedButton(
                  onClick = { showResetConfirm = true },
                  shape = RoundedCornerShape(12.dp),
                  modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                ) {
                  Text(
                    text = stringResource(R.string.settings_reset_stats),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelLarge
                  )
                }
              }
            }
          }

          SettingsCategory.VIDEO -> {
            item { VlcVideoSettingsSection(settings = state.settings, viewModel = viewModel) }
          }

          SettingsCategory.AUDIO -> {
            item { VlcAudioSettingsSection(settings = state.settings, viewModel = viewModel) }
          }

          SettingsCategory.SUBTITLE -> {
            item { VlcSubtitleSettingsSection(settings = state.settings, viewModel = viewModel) }
          }

          SettingsCategory.CONTROLS -> {
            item {
              SettingsSectionCard(title = stringResource(R.string.settings_playback_section)) {
                SettingsChipGroup(
                  title = stringResource(R.string.settings_default_speed),
                  options = speedChoices.map { it to "${it}x" },
                  selectedValue = state.settings.defaultPlaybackSpeed,
                  onSelect = viewModel::setDefaultPlaybackSpeed
                )
                HorizontalDivider(color = ObsidianBorder)
                SettingsChipGroup(
                  title = stringResource(R.string.settings_seek_jump),
                  options = listOf(5, 10, 15, 30).map {
                    it to stringResource(R.string.settings_seek_jump_value, it)
                  },
                  selectedValue = state.settings.seekJumpSeconds,
                  onSelect = viewModel::setSeekJumpSeconds
                )
                HorizontalDivider(color = ObsidianBorder)
                SettingsChipGroup(
                  title = stringResource(R.string.settings_autohide_delay),
                  options = listOf(
                    2 to stringResource(R.string.settings_autohide_value, 2),
                    3 to stringResource(R.string.settings_autohide_value, 3),
                    5 to stringResource(R.string.settings_autohide_value, 5),
                    0 to stringResource(R.string.settings_autohide_manual)
                  ),
                  selectedValue = state.settings.controlsAutoHideSeconds,
                  onSelect = viewModel::setControlsAutoHideSeconds
                )
                HorizontalDivider(color = ObsidianBorder)
                SettingsChipGroup(
                  title = stringResource(R.string.settings_skip_op_duration),
                  options = listOf(75, 85, 90).map {
                    it to stringResource(R.string.settings_seek_jump_value, it)
                  },
                  selectedValue = state.settings.skipIntroDurationSeconds,
                  onSelect = viewModel::setSkipIntroDurationSeconds
                )
                HorizontalDivider(color = ObsidianBorder)
                SettingsToggleRow(
                  title = stringResource(R.string.settings_gestures_toggle),
                  subtitle = stringResource(R.string.settings_gestures_desc),
                  checked = state.settings.enableBrightnessVolumeGestures,
                  onCheckedChange = viewModel::setEnableBrightnessVolumeGestures
                )
                HorizontalDivider(color = ObsidianBorder)
                SettingsToggleRow(
                  title = stringResource(R.string.settings_horizontal_seek),
                  subtitle = stringResource(R.string.settings_horizontal_seek_desc),
                  checked = state.settings.enableHorizontalSwipeSeek,
                  onCheckedChange = viewModel::setEnableHorizontalSwipeSeek
                )
                HorizontalDivider(color = ObsidianBorder)
                SettingsToggleRow(
                  title = stringResource(R.string.settings_double_tap_toggle),
                  subtitle = stringResource(R.string.settings_double_tap_desc),
                  checked = state.settings.enableDoubleTapSeek,
                  onCheckedChange = viewModel::setEnableDoubleTapSeek
                )
                HorizontalDivider(color = ObsidianBorder)
                SettingsToggleRow(
                  title = stringResource(R.string.settings_auto_scan_intro),
                  subtitle = stringResource(R.string.settings_auto_scan_intro_desc),
                  checked = state.settings.autoScanIntroOutro,
                  onCheckedChange = viewModel::setAutoScanIntroOutro
                )
                HorizontalDivider(color = ObsidianBorder)
                SettingsToggleRow(
                  title = stringResource(R.string.settings_auto_skip_intro),
                  subtitle = stringResource(R.string.settings_auto_skip_intro_desc),
                  checked = state.settings.autoSkipIntro,
                  onCheckedChange = viewModel::setAutoSkipIntro
                )
                HorizontalDivider(color = ObsidianBorder)
                SettingsToggleRow(
                  title = stringResource(R.string.settings_auto_skip_outro),
                  subtitle = stringResource(R.string.settings_auto_skip_outro_desc),
                  checked = state.settings.autoSkipOutro,
                  onCheckedChange = viewModel::setAutoSkipOutro
                )
                HorizontalDivider(color = ObsidianBorder)
                SettingsToggleRow(
                  title = stringResource(R.string.settings_clock_battery),
                  subtitle = stringResource(R.string.settings_clock_battery_desc),
                  checked = state.settings.showClockAndBatteryInPlayer,
                  onCheckedChange = viewModel::setShowClockAndBatteryInPlayer
                )
                HorizontalDivider(color = ObsidianBorder)
                SettingsChipGroup(
                  title = stringResource(R.string.player_repeat_mode_title),
                  options = listOf(
                    "off" to stringResource(R.string.player_repeat_off),
                    "one" to stringResource(R.string.player_repeat_one),
                    "all" to stringResource(R.string.player_repeat_all)
                  ),
                  selectedValue = state.settings.defaultRepeatMode,
                  onSelect = viewModel::setDefaultRepeatMode
                )
                HorizontalDivider(color = ObsidianBorder)
                SettingsToggleRow(
                  title = stringResource(R.string.settings_autoplay_next),
                  subtitle = stringResource(R.string.settings_autoplay_next_desc),
                  checked = state.settings.autoPlayNextEpisode,
                  onCheckedChange = viewModel::setAutoPlayNextEpisode
                )
                HorizontalDivider(color = ObsidianBorder)
                SettingsChipGroup(
                  title = stringResource(R.string.settings_watched_threshold),
                  options = thresholdChoices.map {
                    it to stringResource(R.string.settings_watched_threshold_value, it)
                  },
                  selectedValue = state.settings.watchedThresholdPercent,
                  onSelect = viewModel::setWatchedThresholdPercent
                )
              }
            }
          }

          SettingsCategory.ADVANCED -> {
            item {
              SettingsSectionCard(title = stringResource(R.string.settings_advanced_vlc_section)) {
                SettingsChipGroup(
                  title = stringResource(R.string.settings_buffer_profile),
                  options = listOf(
                    "low_ram" to stringResource(R.string.settings_buffer_low_ram),
                    "balanced" to stringResource(R.string.settings_buffer_balanced),
                    "high" to stringResource(R.string.settings_buffer_high)
                  ),
                  selectedValue = state.settings.bufferProfile,
                  onSelect = viewModel::setBufferProfile
                )
                HorizontalDivider(color = ObsidianBorder)
                SettingsToggleRow(
                  title = stringResource(R.string.settings_save_history),
                  subtitle = stringResource(R.string.settings_save_history_desc),
                  checked = state.settings.saveWatchHistory,
                  onCheckedChange = viewModel::setSaveWatchHistory
                )
                HorizontalDivider(color = ObsidianBorder)
                OutlinedButton(
                  onClick = { viewModel.resetPlaybackDefaults(vlcResetMsg) },
                  shape = RoundedCornerShape(12.dp),
                  modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                ) {
                  Text(
                    text = stringResource(R.string.settings_reset_vlc_defaults),
                    color = VlcOrange,
                    style = MaterialTheme.typography.labelLarge
                  )
                }
              }
            }
          }
        }
      }
    }

    SnackbarHost(
      hostState = snackbarHostState,
      modifier = Modifier.align(Alignment.BottomCenter)
    )
  }

  if (showResetConfirm) {
    AlertDialog(
      onDismissRequest = { showResetConfirm = false },
      containerColor = ObsidianSurface,
      title = {
        Text(
          text = stringResource(R.string.settings_reset_stats_confirm_title),
          style = MaterialTheme.typography.titleLarge
        )
      },
      text = {
        Text(
          text = stringResource(R.string.settings_reset_stats_confirm_body),
          style = MaterialTheme.typography.bodyMedium,
          color = SecondaryGray
        )
      },
      confirmButton = {
        Button(
          onClick = {
            viewModel.resetWatchStatistics(statsResetMsg)
            showResetConfirm = false
          },
          colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.error,
            contentColor = MaterialTheme.colorScheme.onError
          )
        ) {
          Text(stringResource(R.string.action_delete))
        }
      },
      dismissButton = {
        TextButton(onClick = { showResetConfirm = false }) {
          Text(stringResource(R.string.action_cancel), color = SecondaryGray)
        }
      }
    )
  }
}
