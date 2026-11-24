package com.example.ui.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.preferences.AppSettings
import com.example.data.preferences.AppSettingsDataStore
import com.example.data.repository.AnimeRepository
import com.example.data.scanner.MediaStoreScanner
import com.example.util.MalXmlExporter
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class FolderEntryUi(
  val key: String,
  val displayLabel: String,
  val isTreeUri: Boolean
)

data class SettingsUiState(
  val settings: AppSettings = AppSettings(),
  val folders: List<FolderEntryUi> = emptyList(),
  val isScanning: Boolean = false,
  val statusMessage: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
  @ApplicationContext private val context: Context,
  private val settingsDataStore: AppSettingsDataStore,
  private val repository: AnimeRepository
) : ViewModel() {

  private val isScanning = MutableStateFlow(false)
  private val statusMessage = MutableStateFlow<String?>(null)

  val uiState: StateFlow<SettingsUiState> = combine(
    settingsDataStore.settingsFlow,
    isScanning,
    statusMessage
  ) { settings, scanning, message ->
    val folderItems = buildList {
      settings.scanFolders.sorted().forEach { rel ->
        add(FolderEntryUi(key = rel, displayLabel = rel, isTreeUri = false))
      }
      settings.scanTreeUris.sorted().forEach { uriStr ->
        val rel = MediaStoreScanner.extractRelativePathFromTreeUri(uriStr)
        if (rel == null || settings.scanFolders.none { it.equals(rel, ignoreCase = true) }) {
          add(
            FolderEntryUi(
              key = uriStr,
              displayLabel = MediaStoreScanner.extractDisplayFolderLabel(uriStr),
              isTreeUri = true
            )
          )
        }
      }
    }

    SettingsUiState(
      settings = settings,
      folders = folderItems,
      isScanning = scanning,
      statusMessage = message
    )
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = SettingsUiState()
  )

  fun addFolderFromTreeUri(uriString: String) {
    viewModelScope.launch {
      val rel = MediaStoreScanner.extractRelativePathFromTreeUri(uriString)
      settingsDataStore.addScanFolder(relativePath = rel ?: "", treeUri = uriString)
      scanNow()
    }
  }

  fun removeFolder(entry: FolderEntryUi) {
    viewModelScope.launch {
      if (entry.isTreeUri) {
        settingsDataStore.removeScanTreeUri(entry.key)
      } else {
        settingsDataStore.removeScanFolder(entry.key)
        uiState.value.settings.scanTreeUris.forEach { treeUri ->
          val rel = MediaStoreScanner.extractRelativePathFromTreeUri(treeUri)
          if (rel != null && rel.equals(entry.key, ignoreCase = true)) {
            settingsDataStore.removeScanTreeUri(treeUri)
          }
        }
      }
      scanNow()
    }
  }

  fun scanNow() {
    if (isScanning.value) return
    viewModelScope.launch {
      isScanning.value = true
      runCatching {
        repository.scanConfiguredFolders()
      }.onFailure { err ->
        statusMessage.value = err.localizedMessage
      }
      isScanning.value = false
    }
  }

  fun setAutoEnrichMal(enabled: Boolean) {
    viewModelScope.launch { settingsDataStore.setAutoEnrichMal(enabled) }
  }

  fun setDefaultPlaybackSpeed(speed: Float) {
    viewModelScope.launch { settingsDataStore.setDefaultPlaybackSpeed(speed) }
  }

  fun setAutoPlayNextEpisode(enabled: Boolean) {
    viewModelScope.launch { settingsDataStore.setAutoPlayNextEpisode(enabled) }
  }

  fun setAutoEnableSubtitles(enabled: Boolean) {
    viewModelScope.launch { settingsDataStore.setAutoEnableSubtitles(enabled) }
  }

  fun setWatchedThresholdPercent(percent: Int) {
    viewModelScope.launch { settingsDataStore.setWatchedThresholdPercent(percent) }
  }

  fun setHardwareAccelerationMode(mode: String) {
    viewModelScope.launch { settingsDataStore.setHardwareAccelerationMode(mode) }
  }

  fun setDefaultAspectRatio(ratio: String) {
    viewModelScope.launch { settingsDataStore.setDefaultAspectRatio(ratio) }
  }

  fun setPlayerOrientationMode(mode: String) {
    viewModelScope.launch { settingsDataStore.setPlayerOrientationMode(mode) }
  }

  fun setEnablePictureInPicture(enabled: Boolean) {
    viewModelScope.launch { settingsDataStore.setEnablePictureInPicture(enabled) }
  }

  fun setKeepScreenOn(enabled: Boolean) {
    viewModelScope.launch { settingsDataStore.setKeepScreenOn(enabled) }
  }

  fun setAudioEqualizerPreset(preset: String) {
    viewModelScope.launch { settingsDataStore.setAudioEqualizerPreset(preset) }
  }

  fun setAudioBoostPercent(percent: Int) {
    viewModelScope.launch { settingsDataStore.setAudioBoostPercent(percent) }
  }

  fun setSkipSilence(enabled: Boolean) {
    viewModelScope.launch { settingsDataStore.setSkipSilence(enabled) }
  }

  fun setPreferredAudioLanguage(lang: String) {
    viewModelScope.launch { settingsDataStore.setPreferredAudioLanguage(lang) }
  }

  fun setSubtitleFontSizeSp(sizeSp: Int) {
    viewModelScope.launch { settingsDataStore.setSubtitleFontSizeSp(sizeSp) }
  }

  fun setSubtitleColorPreset(color: String) {
    viewModelScope.launch { settingsDataStore.setSubtitleColorPreset(color) }
  }

  fun setSubtitleBackgroundBox(enabled: Boolean) {
    viewModelScope.launch { settingsDataStore.setSubtitleBackgroundBox(enabled) }
  }

  fun setSeekJumpSeconds(seconds: Int) {
    viewModelScope.launch { settingsDataStore.setSeekJumpSeconds(seconds) }
  }

  fun setControlsAutoHideSeconds(seconds: Int) {
    viewModelScope.launch { settingsDataStore.setControlsAutoHideSeconds(seconds) }
  }

  fun setEnableBrightnessVolumeGestures(enabled: Boolean) {
    viewModelScope.launch { settingsDataStore.setEnableBrightnessVolumeGestures(enabled) }
  }

  fun setEnableDoubleTapSeek(enabled: Boolean) {
    viewModelScope.launch { settingsDataStore.setEnableDoubleTapSeek(enabled) }
  }

  fun setShowClockAndBatteryInPlayer(enabled: Boolean) {
    viewModelScope.launch { settingsDataStore.setShowClockAndBatteryInPlayer(enabled) }
  }

  fun setSkipIntroDurationSeconds(seconds: Int) {
    viewModelScope.launch { settingsDataStore.setSkipIntroDurationSeconds(seconds) }
  }

  fun setBufferProfile(profile: String) {
    viewModelScope.launch { settingsDataStore.setBufferProfile(profile) }
  }

  fun setSaveWatchHistory(enabled: Boolean) {
    viewModelScope.launch { settingsDataStore.setSaveWatchHistory(enabled) }
  }

  fun resetPlaybackDefaults(doneMessage: String) {
    viewModelScope.launch {
      settingsDataStore.resetPlaybackDefaults()
      statusMessage.value = doneMessage
    }
  }

  fun exportMalXml(targetUri: Uri, successMessage: String) {
    viewModelScope.launch {
      withContext(Dispatchers.IO) {
        val library = repository.getLibrarySnapshot()
        val xml = MalXmlExporter.generateMalExportXml(library)
        context.contentResolver.openOutputStream(targetUri)?.use { out ->
          out.write(xml.toByteArray(Charsets.UTF_8))
        }
      }
      statusMessage.value = successMessage
    }
  }

  fun clearThumbnailCache(doneMessage: String) {
    viewModelScope.launch {
      withContext(Dispatchers.IO) {
        context.cacheDir.resolve("video_thumbs_cache").deleteRecursively()
      }
      statusMessage.value = doneMessage
    }
  }

  fun resetWatchStatistics(doneMessage: String) {
    viewModelScope.launch {
      repository.clearWatchStatistics()
      statusMessage.value = doneMessage
    }
  }

  fun consumeMessage() {
    statusMessage.value = null
  }
}
