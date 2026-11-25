package com.example.ui.player

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MimeTypes
import androidx.navigation.toRoute
import com.example.data.local.entity.EpisodeBookmarkEntity
import com.example.data.local.entity.EpisodeEntity
import com.example.data.preferences.AppSettingsDataStore
import com.example.data.repository.AnimeRepository
import com.example.ui.navigation.PlayerRoute
import com.example.util.SubtitleSyncHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

data class PlayerUiState(
  val isLoadingEpisode: Boolean = true,
  val animeId: Long = 0L,
  val animeTitle: String = "",
  val currentEpisode: EpisodeEntity? = null,
  val nextEpisode: EpisodeEntity? = null,
  val allAnimeEpisodes: List<EpisodeEntity> = emptyList(),
  val requestedStartPositionMs: Long = -1L,
  val subtitleUri: String? = null,
  val effectiveSubtitleUri: String? = null,
  val subtitleMimeType: String = MimeTypes.APPLICATION_SUBRIP,
  val subtitleDisplayName: String? = null,
  val subtitlesEnabled: Boolean = true,
  val subtitleDelayMs: Int = 0,
  val playbackSpeed: Float = 1.0f,
  val autoPlayNextEpisode: Boolean = true,
  val watchedThresholdPercent: Int = 90,
  // VLC Mobile Advanced Settings
  val hardwareAccelerationMode: String = "auto",
  val aspectRatioMode: String = "fit",
  val orientationMode: String = "sensor_landscape",
  val enablePictureInPicture: Boolean = true,
  val keepScreenOn: Boolean = true,
  val audioEqualizerPreset: String = "normal",
  val audioBoostPercent: Int = 100,
  val skipSilence: Boolean = false,
  val preferredAudioLanguage: String = "jpn",
  val subtitleFontSizeSp: Int = 18,
  val subtitleColorPreset: String = "white",
  val subtitleBackgroundBox: Boolean = true,
  val seekJumpSeconds: Int = 10,
  val controlsAutoHideSeconds: Int = 3,
  val enableBrightnessVolumeGestures: Boolean = true,
  val enableHorizontalSwipeSeek: Boolean = true,
  val enablePinchZoom: Boolean = true,
  val enableDoubleTapSeek: Boolean = true,
  val showClockAndBattery: Boolean = true,
  val skipIntroDurationSeconds: Int = 85,
  val bufferProfile: String = "balanced",
  val autoSkipIntro: Boolean = false,
  val autoSkipOutro: Boolean = false,
  val backgroundAudioPlay: Boolean = false,
  val nightDimmerPercent: Int = 0,
  val repeatMode: String = "off",
  val isScanningIntroOutro: Boolean = false,
  val introOutroScanMessage: String? = null,
  val bookmarks: List<EpisodeBookmarkEntity> = emptyList(),
  val errorMessage: String? = null
)

@HiltViewModel
class PlayerViewModel @Inject constructor(
  savedStateHandle: SavedStateHandle,
  @ApplicationContext private val context: Context,
  private val repository: AnimeRepository,
  private val settingsDataStore: AppSettingsDataStore,
  private val contentResolver: ContentResolver
) : ViewModel() {

  private val initialRoute: PlayerRoute = savedStateHandle.toRoute()

  private val _uiState = MutableStateFlow(
    PlayerUiState(requestedStartPositionMs = initialRoute.startPositionMs)
  )
  val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

  init {
    loadEpisode(initialRoute.episodeId, initialRoute.startPositionMs)
  }

  fun loadEpisode(episodeId: Long, explicitStartPosMs: Long = -1L) {
    viewModelScope.launch {
      _uiState.update {
        it.copy(
          isLoadingEpisode = true,
          errorMessage = null,
          introOutroScanMessage = null,
          requestedStartPositionMs = explicitStartPosMs
        )
      }
      val settings = settingsDataStore.settingsFlow.first()
      val withAnime = repository.getEpisodeWithAnime(episodeId)
      if (withAnime == null) {
        _uiState.update {
          it.copy(
            isLoadingEpisode = false,
            errorMessage = "not_found"
          )
        }
        return@launch
      }

      val episode = withAnime.episode
      val allEpisodes = repository.observeEpisodes(episode.animeId).first()
      val next = repository.getNextEpisode(episode.animeId, episode.number)
      val resolvedSub = episode.subtitleUri
      val subName = resolvedSub?.let { resolveDisplayName(it) }
      val subMime = resolvedSub?.let { resolveSubtitleMimeType(it, subName) }
        ?: MimeTypes.APPLICATION_SUBRIP
      val effectiveSub = resolveEffectiveSubtitleUri(
        sourceUri = resolvedSub,
        mimeType = subMime,
        episodeId = episode.id,
        delayMs = settings.subtitleDelayMs
      )
      val bookmarks = repository.observeBookmarksForEpisode(episode.id).first()

      _uiState.update {
        it.copy(
          isLoadingEpisode = false,
          animeId = withAnime.anime.id,
          animeTitle = withAnime.anime.title,
          currentEpisode = episode,
          nextEpisode = next,
          allAnimeEpisodes = allEpisodes,
          requestedStartPositionMs = explicitStartPosMs,
          subtitleUri = resolvedSub,
          effectiveSubtitleUri = effectiveSub,
          subtitleMimeType = subMime,
          subtitleDisplayName = subName,
          subtitlesEnabled = settings.autoEnableSubtitles,
          subtitleDelayMs = settings.subtitleDelayMs,
          playbackSpeed = settings.defaultPlaybackSpeed,
          autoPlayNextEpisode = settings.autoPlayNextEpisode,
          watchedThresholdPercent = settings.watchedThresholdPercent,
          hardwareAccelerationMode = settings.hardwareAccelerationMode,
          aspectRatioMode = settings.defaultAspectRatio,
          orientationMode = settings.playerOrientationMode,
          enablePictureInPicture = settings.enablePictureInPicture,
          keepScreenOn = settings.keepScreenOn,
          audioEqualizerPreset = settings.audioEqualizerPreset,
          audioBoostPercent = settings.audioBoostPercent,
          skipSilence = settings.skipSilence,
          preferredAudioLanguage = settings.preferredAudioLanguage,
          subtitleFontSizeSp = settings.subtitleFontSizeSp,
          subtitleColorPreset = settings.subtitleColorPreset,
          subtitleBackgroundBox = settings.subtitleBackgroundBox,
          seekJumpSeconds = settings.seekJumpSeconds,
          controlsAutoHideSeconds = settings.controlsAutoHideSeconds,
          enableBrightnessVolumeGestures = settings.enableBrightnessVolumeGestures,
          enableHorizontalSwipeSeek = settings.enableHorizontalSwipeSeek,
          enablePinchZoom = settings.enablePinchZoom,
          enableDoubleTapSeek = settings.enableDoubleTapSeek,
          showClockAndBattery = settings.showClockAndBatteryInPlayer,
          skipIntroDurationSeconds = settings.skipIntroDurationSeconds,
          bufferProfile = settings.bufferProfile,
          autoSkipIntro = settings.autoSkipIntro,
          autoSkipOutro = settings.autoSkipOutro,
          backgroundAudioPlay = settings.backgroundAudioPlay,
          nightDimmerPercent = settings.nightDimmerPercent,
          repeatMode = settings.defaultRepeatMode,
          bookmarks = bookmarks,
          errorMessage = null
        )
      }
    }
  }

  private suspend fun resolveEffectiveSubtitleUri(
    sourceUri: String?,
    mimeType: String,
    episodeId: Long,
    delayMs: Int
  ): String? = withContext(Dispatchers.IO) {
    if (sourceUri.isNullOrBlank()) return@withContext null
    if (delayMs == 0) return@withContext sourceUri
    val isAss = mimeType == MimeTypes.TEXT_SSA
    SubtitleSyncHelper.createShiftedSubtitleUri(
      context = context,
      contentResolver = contentResolver,
      sourceUri = sourceUri,
      isAss = isAss,
      episodeId = episodeId,
      offsetMs = delayMs.toLong()
    ) ?: sourceUri
  }

  fun saveProgress(positionMs: Long, durationMs: Long, watchedDeltaMs: Long = 0L) {
    val episodeId = _uiState.value.currentEpisode?.id ?: return
    viewModelScope.launch {
      repository.savePlaybackProgress(
        episodeId = episodeId,
        positionMs = positionMs,
        durationMs = durationMs,
        watchedDeltaMs = watchedDeltaMs
      )
    }
  }

  fun toggleSubtitles(): Boolean {
    val nextEnabled = !_uiState.value.subtitlesEnabled
    _uiState.update { it.copy(subtitlesEnabled = nextEnabled) }
    return nextEnabled
  }

  fun setPlaybackSpeed(speed: Float) {
    _uiState.update { it.copy(playbackSpeed = speed) }
  }

  fun cycleAspectRatio(): String {
    val order = listOf("fit", "fill", "zoom", "16_9", "4_3")
    val current = _uiState.value.aspectRatioMode
    val next = order[(order.indexOf(current) + 1) % order.size]
    _uiState.update { it.copy(aspectRatioMode = next) }
    return next
  }

  fun setEqualizerPreset(preset: String) {
    _uiState.update { it.copy(audioEqualizerPreset = preset) }
    viewModelScope.launch { settingsDataStore.setAudioEqualizerPreset(preset) }
  }

  fun setAudioBoostPercent(percent: Int) {
    val clamped = percent.coerceIn(100, 200)
    _uiState.update { it.copy(audioBoostPercent = clamped) }
    viewModelScope.launch { settingsDataStore.setAudioBoostPercent(clamped) }
  }

  fun setRepeatMode(mode: String) {
    _uiState.update { it.copy(repeatMode = mode) }
    viewModelScope.launch { settingsDataStore.setDefaultRepeatMode(mode) }
  }

  fun setNightDimmerPercent(percent: Int) {
    val clamped = percent.coerceIn(0, 60)
    _uiState.update { it.copy(nightDimmerPercent = clamped) }
    viewModelScope.launch { settingsDataStore.setNightDimmerPercent(clamped) }
  }

  fun setAutoSkipIntro(enabled: Boolean) {
    _uiState.update { it.copy(autoSkipIntro = enabled) }
    viewModelScope.launch { settingsDataStore.setAutoSkipIntro(enabled) }
  }

  fun setAutoSkipOutro(enabled: Boolean) {
    _uiState.update { it.copy(autoSkipOutro = enabled) }
    viewModelScope.launch { settingsDataStore.setAutoSkipOutro(enabled) }
  }

  fun setBackgroundAudioPlay(enabled: Boolean) {
    _uiState.update { it.copy(backgroundAudioPlay = enabled) }
    viewModelScope.launch { settingsDataStore.setBackgroundAudioPlay(enabled) }
  }

  fun setSubtitleDelayMs(delayMs: Int) {
    val clamped = delayMs.coerceIn(-5000, 5000)
    val ep = _uiState.value.currentEpisode
    val srcSub = _uiState.value.subtitleUri
    val mime = _uiState.value.subtitleMimeType
    viewModelScope.launch {
      settingsDataStore.setSubtitleDelayMs(clamped)
      val shifted = if (ep != null) {
        resolveEffectiveSubtitleUri(srcSub, mime, ep.id, clamped)
      } else {
        srcSub
      }
      _uiState.update {
        it.copy(
          subtitleDelayMs = clamped,
          effectiveSubtitleUri = shifted
        )
      }
    }
  }

  fun scanCurrentEpisodeIntroOutro(foundMsgFormat: String, noneMsg: String) {
    val ep = _uiState.value.currentEpisode ?: return
    if (_uiState.value.isScanningIntroOutro) return
    viewModelScope.launch {
      _uiState.update { it.copy(isScanningIntroOutro = true, introOutroScanMessage = null) }
      val result = repository.scanIntroOutroForEpisode(ep.id)
      val updatedEp = repository.getEpisodeWithAnime(ep.id)?.episode ?: ep
      val allEps = repository.observeEpisodes(ep.animeId).first()
      val msg = if (result.hasAny) {
        String.format(foundMsgFormat, result.source)
      } else {
        noneMsg
      }
      _uiState.update {
        it.copy(
          isScanningIntroOutro = false,
          currentEpisode = updatedEp,
          allAnimeEpisodes = allEps,
          introOutroScanMessage = msg
        )
      }
    }
  }

  fun saveIntroOutroSegments(
    introStartMs: Long,
    introEndMs: Long,
    outroStartMs: Long,
    outroEndMs: Long,
    source: String,
    applyToAllEpisodes: Boolean
  ) {
    val ep = _uiState.value.currentEpisode ?: return
    viewModelScope.launch {
      repository.saveIntroOutro(
        episodeId = ep.id,
        animeId = ep.animeId,
        introStartMs = introStartMs,
        introEndMs = introEndMs,
        outroStartMs = outroStartMs,
        outroEndMs = outroEndMs,
        source = source,
        applyToAllEpisodes = applyToAllEpisodes
      )
      val updatedEp = repository.getEpisodeWithAnime(ep.id)?.episode ?: ep
      val allEps = repository.observeEpisodes(ep.animeId).first()
      _uiState.update {
        it.copy(
          currentEpisode = updatedEp,
          allAnimeEpisodes = allEps
        )
      }
    }
  }

  fun addBookmarkAt(positionMs: Long, label: String) {
    val ep = _uiState.value.currentEpisode ?: return
    viewModelScope.launch {
      repository.addBookmark(
        episodeId = ep.id,
        animeId = ep.animeId,
        episodeNumber = ep.number,
        positionMs = positionMs,
        label = label
      )
      val refreshed = repository.observeBookmarksForEpisode(ep.id).first()
      _uiState.update { it.copy(bookmarks = refreshed) }
    }
  }

  fun onSubtitlePicked(uriString: String) {
    val episodeId = _uiState.value.currentEpisode?.id ?: return
    val name = resolveDisplayName(uriString)
    val mime = resolveSubtitleMimeType(uriString, name)
    viewModelScope.launch {
      repository.updateSubtitleUri(episodeId, uriString)
      val effective = resolveEffectiveSubtitleUri(
        sourceUri = uriString,
        mimeType = mime,
        episodeId = episodeId,
        delayMs = _uiState.value.subtitleDelayMs
      )
      _uiState.update {
        it.copy(
          subtitleUri = uriString,
          effectiveSubtitleUri = effective,
          subtitleDisplayName = name,
          subtitleMimeType = mime,
          subtitlesEnabled = true
        )
      }
    }
  }

  suspend fun captureFrameToPictures(positionMs: Long): Boolean = withContext(Dispatchers.IO) {
    val ep = _uiState.value.currentEpisode ?: return@withContext false
    val retriever = MediaMetadataRetriever()
    val bitmap = runCatching {
      retriever.setDataSource(context, Uri.parse(ep.fileUri))
      retriever.getFrameAtTime(positionMs.coerceAtLeast(0L) * 1000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
    }.getOrNull()
    runCatching { retriever.release() }
    if (bitmap == null) return@withContext false

    val fileName = "AnimePlay_Ep${ep.number}_${System.currentTimeMillis()}.png"
    return@withContext runCatching {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val values = ContentValues().apply {
          put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
          put(MediaStore.Images.Media.MIME_TYPE, "image/png")
          put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/AnimePlay")
          put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val imageUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
          ?: return@runCatching false
        contentResolver.openOutputStream(imageUri)?.use { out ->
          bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        values.clear()
        values.put(MediaStore.Images.Media.IS_PENDING, 0)
        contentResolver.update(imageUri, values, null, null)
        true
      } else {
        val dir = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "AnimePlay").apply {
          mkdirs()
        }
        val outFile = File(dir, fileName)
        FileOutputStream(outFile).use { out ->
          bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        true
      }
    }.getOrDefault(false).also {
      bitmap.recycle()
    }
  }

  private fun resolveDisplayName(uriString: String): String {
    val uri = Uri.parse(uriString)
    if (uri.scheme == "content") {
      runCatching {
        contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
          ?.use { cursor ->
            if (cursor.moveToFirst()) {
              val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
              if (idx >= 0) return cursor.getString(idx)
            }
          }
      }
    }
    return uri.lastPathSegment?.substringAfterLast('/') ?: "subtitle.srt"
  }

  private fun resolveSubtitleMimeType(uriString: String, displayName: String?): String {
    val candidate = (displayName ?: uriString).lowercase()
    return if (candidate.endsWith(".ass") || candidate.endsWith(".ssa")) {
      MimeTypes.TEXT_SSA
    } else {
      MimeTypes.APPLICATION_SUBRIP
    }
  }
}
