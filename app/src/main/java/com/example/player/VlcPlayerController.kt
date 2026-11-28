package com.example.player

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import com.example.R
import com.example.ui.player.PlayerTrackOption
import com.example.util.EqualizerPresets
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.interfaces.IMedia
import org.videolan.libvlc.util.VLCVideoLayout
import java.io.File
import java.io.FileOutputStream

data class VlcMediaInfo(
  val resolution: String = "",
  val videoCodec: String = "",
  val audioCodec: String = ""
)

class VlcPlayerController(
  private val context: Context,
  private val contentResolver: ContentResolver
) {

  private data class OpenRequest(
    val fileUri: String,
    val startPositionMs: Long,
    val playbackSpeed: Float,
    val hardwareAccelerationMode: String,
    val bufferProfile: String,
    val preferredAudioLanguage: String,
    val subtitlesEnabled: Boolean,
    val externalSubtitleUri: String?,
    val subtitleDelayMs: Int,
    val equalizerPreset: String,
    val audioBoostPercent: Int,
    val aspectRatioMode: String
  )

  private val mainHandler = Handler(Looper.getMainLooper())

  private var subtitleFontSizeSp: Int = 18
  private var subtitleColorPreset: String = "white"
  private var subtitleBackgroundBox: Boolean = true

  private var libVLC: LibVLC = createLibVlc()
  private var mediaPlayer: MediaPlayer = createMediaPlayer(libVLC)
  private var currentPfd: ParcelFileDescriptor? = null
  private var attachedLayout: VLCVideoLayout? = null
  private var isDisposed = false

  private var pendingSeekMs: Long = -1L
  private var pendingSpeed: Float = 1.0f
  private var pendingPreferredAudioLang: String = "auto"
  private var pendingSubtitlesEnabled: Boolean = true
  private var pendingExternalSubUri: String? = null
  private var pendingSubtitleDelayMs: Int = 0
  private var pendingEqPreset: String = "normal"
  private var pendingAudioBoostPercent: Int = 100
  private var currentAspectRatioMode: String = "fit"
  private var appliedInitialTracksForCurrentMedia = false
  private var lastOpenRequest: OpenRequest? = null

  private val _isPlaying = MutableStateFlow(false)
  val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

  private val _positionMs = MutableStateFlow(0L)
  val positionMs: StateFlow<Long> = _positionMs.asStateFlow()

  private val _durationMs = MutableStateFlow(0L)
  val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

  private val _isBuffering = MutableStateFlow(false)
  val isBuffering: StateFlow<Boolean> = _isBuffering.asStateFlow()

  private val _error = MutableStateFlow<String?>(null)
  val error: StateFlow<String?> = _error.asStateFlow()

  private val _audioTracks = MutableStateFlow<List<PlayerTrackOption>>(emptyList())
  val audioTracks: StateFlow<List<PlayerTrackOption>> = _audioTracks.asStateFlow()

  private val _subtitleTracks = MutableStateFlow<List<PlayerTrackOption>>(emptyList())
  val subtitleTracks: StateFlow<List<PlayerTrackOption>> = _subtitleTracks.asStateFlow()

  private val _mediaInfo = MutableStateFlow(VlcMediaInfo())
  val mediaInfo: StateFlow<VlcMediaInfo> = _mediaInfo.asStateFlow()

  private val _endReachedCount = MutableStateFlow(0L)
  val endReachedCount: StateFlow<Long> = _endReachedCount.asStateFlow()

  private fun createLibVlc(): LibVLC {
    val textScale = when (subtitleFontSizeSp) {
      14 -> 80
      22 -> 125
      26 -> 150
      else -> 100
    }
    val freetypeColor = when (subtitleColorPreset) {
      "yellow" -> 16776960
      "orange" -> 16746496
      "cyan" -> 65535
      else -> 16777215
    }
    val bgOpacity = if (subtitleBackgroundBox) 145 else 0
    val args = arrayListOf(
      "--no-video-title-show",
      "--audio-time-stretch",
      "--sub-text-scale=$textScale",
      "--freetype-color=$freetypeColor",
      "--freetype-background-opacity=$bgOpacity"
    )
    return LibVLC(context, args)
  }

  private fun createMediaPlayer(vlc: LibVLC): MediaPlayer {
    return MediaPlayer(vlc).apply {
      setEventListener { event ->
        handleVlcEvent(event)
      }
    }
  }

  private fun handleVlcEvent(event: MediaPlayer.Event) {
    when (event.type) {
      MediaPlayer.Event.Opening -> {
        _isBuffering.value = true
      }
      MediaPlayer.Event.Buffering -> {
        _isBuffering.value = event.buffering < 100f
      }
      MediaPlayer.Event.Playing -> {
        _isPlaying.value = true
        _isBuffering.value = false
        _error.value = null
        mainHandler.post {
          if (isDisposed) return@post
          if (pendingSeekMs > 0L) {
            val target = pendingSeekMs
            pendingSeekMs = -1L
            runCatching { mediaPlayer.time = target }
            _positionMs.value = target
          }
          runCatching { mediaPlayer.rate = pendingSpeed }
          applyEqualizerAndBoostInternal(pendingEqPreset, pendingAudioBoostPercent)
          runCatching { mediaPlayer.spuDelay = pendingSubtitleDelayMs * 1000L }
          applyScaleTypeInternal(currentAspectRatioMode)

          val len = runCatching { mediaPlayer.length }.getOrDefault(0L)
          if (len > 0L) _durationMs.value = len

          if (!appliedInitialTracksForCurrentMedia) {
            appliedInitialTracksForCurrentMedia = true
            applyInitialAudioPreference()
            applyInitialSubtitlePreference()
          }
          refreshTracksInternal()
          refreshMediaInfoInternal()
        }
      }
      MediaPlayer.Event.Paused -> {
        _isPlaying.value = false
        _isBuffering.value = false
      }
      MediaPlayer.Event.Stopped -> {
        _isPlaying.value = false
        _isBuffering.value = false
      }
      MediaPlayer.Event.TimeChanged -> {
        _positionMs.value = event.timeChanged.coerceAtLeast(0L)
        mainHandler.post {
          if (isDisposed) return@post
          val len = runCatching { mediaPlayer.length }.getOrDefault(0L)
          if (len > 0L && _durationMs.value != len) {
            _durationMs.value = len
          }
        }
      }
      MediaPlayer.Event.LengthChanged -> {
        if (event.lengthChanged > 0L) {
          _durationMs.value = event.lengthChanged
        }
      }
      MediaPlayer.Event.Vout -> {
        mainHandler.post {
          if (isDisposed) return@post
          applyScaleTypeInternal(currentAspectRatioMode)
          refreshMediaInfoInternal()
        }
      }
      MediaPlayer.Event.ESAdded,
      MediaPlayer.Event.ESDeleted,
      MediaPlayer.Event.ESSelected -> {
        mainHandler.post {
          if (isDisposed) return@post
          refreshTracksInternal()
          refreshMediaInfoInternal()
        }
      }
      MediaPlayer.Event.EndReached -> {
        _isPlaying.value = false
        _isBuffering.value = false
        _endReachedCount.value = _endReachedCount.value + 1L
      }
      MediaPlayer.Event.EncounteredError -> {
        _isPlaying.value = false
        _isBuffering.value = false
        _error.value = "vlc_playback_error"
      }
    }
  }

  fun configureSubtitleAppearance(
    fontSizeSp: Int,
    colorPreset: String,
    backgroundBox: Boolean
  ) {
    if (isDisposed) return
    if (subtitleFontSizeSp == fontSizeSp &&
      subtitleColorPreset == colorPreset &&
      subtitleBackgroundBox == backgroundBox
    ) {
      return
    }
    subtitleFontSizeSp = fontSizeSp
    subtitleColorPreset = colorPreset
    subtitleBackgroundBox = backgroundBox

    val existingLayout = attachedLayout
    runCatching { mediaPlayer.stop() }
    if (existingLayout != null) {
      runCatching { mediaPlayer.detachViews() }
    }
    runCatching { mediaPlayer.release() }
    runCatching { libVLC.release() }

    libVLC = createLibVlc()
    mediaPlayer = createMediaPlayer(libVLC)
    if (existingLayout != null) {
      attachedLayout = existingLayout
      runCatching { mediaPlayer.attachViews(existingLayout, null, true, true) }
    }
  }

  fun attachView(layout: VLCVideoLayout) {
    if (isDisposed) return
    if (attachedLayout === layout) return
    if (attachedLayout != null) {
      runCatching { mediaPlayer.detachViews() }
    }
    attachedLayout = layout
    runCatching {
      mediaPlayer.attachViews(layout, null, true, true)
      applyScaleTypeInternal(currentAspectRatioMode)
    }
  }

  fun detachView(layout: VLCVideoLayout) {
    if (isDisposed) return
    if (attachedLayout === layout) {
      runCatching { mediaPlayer.detachViews() }
      attachedLayout = null
    }
  }

  fun openMedia(
    fileUri: String,
    startPositionMs: Long,
    playbackSpeed: Float,
    hardwareAccelerationMode: String,
    bufferProfile: String,
    preferredAudioLanguage: String,
    subtitlesEnabled: Boolean,
    externalSubtitleUri: String?,
    subtitleDelayMs: Int,
    equalizerPreset: String,
    audioBoostPercent: Int,
    aspectRatioMode: String
  ) {
    if (isDisposed) return
    val request = OpenRequest(
      fileUri = fileUri,
      startPositionMs = startPositionMs,
      playbackSpeed = playbackSpeed,
      hardwareAccelerationMode = hardwareAccelerationMode,
      bufferProfile = bufferProfile,
      preferredAudioLanguage = preferredAudioLanguage,
      subtitlesEnabled = subtitlesEnabled,
      externalSubtitleUri = externalSubtitleUri,
      subtitleDelayMs = subtitleDelayMs,
      equalizerPreset = equalizerPreset,
      audioBoostPercent = audioBoostPercent,
      aspectRatioMode = aspectRatioMode
    )
    lastOpenRequest = request

    runCatching { mediaPlayer.stop() }
    closeCurrentPfd()

    _error.value = null
    _isBuffering.value = true
    _isPlaying.value = false
    _audioTracks.value = emptyList()
    _subtitleTracks.value = emptyList()
    _mediaInfo.value = VlcMediaInfo()

    pendingSeekMs = startPositionMs.coerceAtLeast(0L)
    _positionMs.value = if (pendingSeekMs > 0L) pendingSeekMs else 0L
    _durationMs.value = 0L
    pendingSpeed = playbackSpeed
    pendingPreferredAudioLang = preferredAudioLanguage
    pendingSubtitlesEnabled = subtitlesEnabled
    pendingExternalSubUri = externalSubtitleUri
    pendingSubtitleDelayMs = subtitleDelayMs
    pendingEqPreset = equalizerPreset
    pendingAudioBoostPercent = audioBoostPercent
    currentAspectRatioMode = aspectRatioMode
    appliedInitialTracksForCurrentMedia = false

    val media = createMediaFromUri(fileUri)
    if (media == null) {
      _isBuffering.value = false
      _error.value = "cannot_open_uri"
      return
    }

    when (hardwareAccelerationMode) {
      "hardware" -> media.setHWDecoderEnabled(true, true)
      "software" -> media.setHWDecoderEnabled(false, false)
      else -> media.setHWDecoderEnabled(true, false)
    }

    val cachingMs = when (bufferProfile) {
      "low_ram" -> 300
      "high" -> 1500
      else -> 600
    }
    media.addOption(":file-caching=$cachingMs")

    runCatching {
      mediaPlayer.media = media
      media.release()
      mediaPlayer.play()
    }.onFailure {
      runCatching { media.release() }
      _isBuffering.value = false
      _error.value = "vlc_start_failed"
    }
  }

  fun replayFromStart() {
    val req = lastOpenRequest ?: return
    openMedia(
      fileUri = req.fileUri,
      startPositionMs = 0L,
      playbackSpeed = pendingSpeed,
      hardwareAccelerationMode = req.hardwareAccelerationMode,
      bufferProfile = req.bufferProfile,
      preferredAudioLanguage = pendingPreferredAudioLang,
      subtitlesEnabled = pendingSubtitlesEnabled,
      externalSubtitleUri = pendingExternalSubUri,
      subtitleDelayMs = pendingSubtitleDelayMs,
      equalizerPreset = pendingEqPreset,
      audioBoostPercent = pendingAudioBoostPercent,
      aspectRatioMode = currentAspectRatioMode
    )
  }

  private fun createMediaFromUri(uriString: String): Media? {
    val uri = Uri.parse(uriString)
    if (uri.scheme == "http" || uri.scheme == "https") {
      return runCatching { Media(libVLC, uri) }.getOrNull()
    }
    val pfd = runCatching {
      when (uri.scheme) {
        "content" -> contentResolver.openFileDescriptor(uri, "r")
        "file" -> uri.path?.let {
          ParcelFileDescriptor.open(File(it), ParcelFileDescriptor.MODE_READ_ONLY)
        }
        else -> {
          val file = File(uriString)
          if (file.exists()) {
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
          } else {
            contentResolver.openFileDescriptor(uri, "r")
          }
        }
      }
    }.getOrNull() ?: return null

    currentPfd = pfd
    return runCatching { Media(libVLC, pfd.fileDescriptor) }.getOrNull()
  }

  fun play() {
    if (isDisposed) return
    runCatching { mediaPlayer.play() }
  }

  fun pause() {
    if (isDisposed) return
    runCatching {
      if (mediaPlayer.isPlaying) {
        mediaPlayer.pause()
      }
    }
  }

  fun seekTo(positionMs: Long) {
    if (isDisposed) return
    val maxDur = _durationMs.value.takeIf { it > 0L } ?: Long.MAX_VALUE
    val clamped = positionMs.coerceIn(0L, maxDur)
    _positionMs.value = clamped
    runCatching { mediaPlayer.time = clamped }
  }

  fun setPlaybackSpeed(speed: Float) {
    pendingSpeed = speed
    if (isDisposed) return
    runCatching { mediaPlayer.rate = speed }
  }

  fun setAspectRatioMode(mode: String) {
    currentAspectRatioMode = mode
    if (isDisposed) return
    applyScaleTypeInternal(mode)
  }

  fun setEqualizerAndBoost(presetKey: String, boostPercent: Int) {
    pendingEqPreset = presetKey
    pendingAudioBoostPercent = boostPercent.coerceIn(0, 200)
    if (isDisposed) return
    applyEqualizerAndBoostInternal(pendingEqPreset, pendingAudioBoostPercent)
  }

  fun selectAudioTrack(trackId: Int) {
    if (isDisposed) return
    runCatching {
      mediaPlayer.setAudioTrack(trackId)
      refreshTracksInternal()
    }
  }

  fun selectSubtitleTrack(trackId: Int) {
    pendingSubtitlesEnabled = trackId != -1
    if (isDisposed) return
    runCatching {
      mediaPlayer.setSpuTrack(trackId)
      refreshTracksInternal()
    }
  }

  fun setSubtitlesEnabled(enabled: Boolean) {
    pendingSubtitlesEnabled = enabled
    if (isDisposed) return
    runCatching {
      if (!enabled) {
        mediaPlayer.setSpuTrack(-1)
      } else {
        val current = mediaPlayer.spuTrack
        if (current == -1) {
          val firstValid = mediaPlayer.spuTracks?.firstOrNull { it != null && it.id != -1 }
          if (firstValid != null) {
            mediaPlayer.setSpuTrack(firstValid.id)
          } else {
            pendingExternalSubUri?.let { addExternalSubtitleInternal(it, select = true) }
          }
        }
      }
      refreshTracksInternal()
    }
  }

  fun setExternalSubtitle(uriString: String?, select: Boolean) {
    pendingExternalSubUri = uriString
    pendingSubtitlesEnabled = select
    if (isDisposed || uriString.isNullOrBlank()) return
    addExternalSubtitleInternal(uriString, select)
  }

  fun setSubtitleDelayMs(delayMs: Int) {
    pendingSubtitleDelayMs = delayMs
    if (isDisposed) return
    runCatching {
      mediaPlayer.spuDelay = delayMs * 1000L
    }
  }

  fun clearError() {
    _error.value = null
  }

  fun release() {
    if (isDisposed) return
    isDisposed = true
    mainHandler.removeCallbacksAndMessages(null)
    runCatching { mediaPlayer.setEventListener(null) }
    runCatching { mediaPlayer.stop() }
    if (attachedLayout != null) {
      runCatching { mediaPlayer.detachViews() }
      attachedLayout = null
    }
    runCatching { mediaPlayer.release() }
    runCatching { libVLC.release() }
    closeCurrentPfd()
  }

  private fun closeCurrentPfd() {
    runCatching { currentPfd?.close() }
    currentPfd = null
  }

  private fun applyScaleTypeInternal(mode: String) {
    if (attachedLayout == null) return
    val scaleType = when (mode) {
      "fill" -> MediaPlayer.ScaleType.SURFACE_FILL
      "zoom" -> MediaPlayer.ScaleType.SURFACE_FIT_SCREEN
      "16_9" -> MediaPlayer.ScaleType.SURFACE_16_9
      "4_3" -> MediaPlayer.ScaleType.SURFACE_4_3
      else -> MediaPlayer.ScaleType.SURFACE_BEST_FIT
    }
    runCatching { mediaPlayer.videoScale = scaleType }
  }

  private fun applyEqualizerAndBoostInternal(presetKey: String, boostPercent: Int) {
    runCatching {
      val eq = if (presetKey != "normal") {
        val bandCount = MediaPlayer.Equalizer.getBandCount()
        MediaPlayer.Equalizer.create().apply {
          for (i in 0 until bandCount) {
            val ampDb = (EqualizerPresets.bandRatio(presetKey, i, bandCount) * 12f).coerceIn(-20f, 20f)
            setAmp(i, ampDb)
          }
        }
      } else {
        null
      }
      mediaPlayer.setEqualizer(eq)
      mediaPlayer.volume = boostPercent.coerceIn(0, 200)
    }
  }

  private fun applyInitialAudioPreference() {
    if (pendingPreferredAudioLang == "auto") return
    val tracks = runCatching { mediaPlayer.audioTracks }.getOrNull() ?: return
    val keywords = when (pendingPreferredAudioLang.lowercase()) {
      "jpn", "ja" -> listOf("jpn", "ja", "japanese", "jepang")
      "eng", "en" -> listOf("eng", "en", "english", "inggris")
      "ind", "id" -> listOf("ind", "id", "indonesian", "indonesia")
      else -> listOf(pendingPreferredAudioLang.lowercase())
    }
    val matched = tracks.firstOrNull { desc ->
      if (desc == null || desc.id == -1) return@firstOrNull false
      val lower = desc.name.orEmpty().lowercase()
      keywords.any { kw -> lower.contains(kw) }
    }
    if (matched != null) {
      runCatching { mediaPlayer.setAudioTrack(matched.id) }
    }
  }

  private fun applyInitialSubtitlePreference() {
    if (!pendingSubtitlesEnabled) {
      runCatching { mediaPlayer.setSpuTrack(-1) }
      return
    }
    val extSub = pendingExternalSubUri
    if (!extSub.isNullOrBlank()) {
      addExternalSubtitleInternal(extSub, select = true)
    }
  }

  private fun addExternalSubtitleInternal(uriString: String, select: Boolean) {
    val playableUri = resolvePlayableSubtitleUri(uriString) ?: return
    runCatching {
      mediaPlayer.addSlave(IMedia.Slave.Type.Subtitle, playableUri, select)
      refreshTracksInternal()
    }
  }

  private fun resolvePlayableSubtitleUri(uriString: String): Uri? {
    val uri = Uri.parse(uriString)
    if (uri.scheme == "file") return uri
    if (uri.scheme != "content") {
      val file = File(uriString)
      return if (file.exists()) Uri.fromFile(file) else uri
    }
    return runCatching {
      val displayName = queryDisplayName(uri) ?: "external.srt"
      val ext = displayName.substringAfterLast('.', "srt").lowercase().ifBlank { "srt" }
      val subDir = File(context.cacheDir, "vlc_sub_cache").apply { mkdirs() }
      val outFile = File(subDir, "active_sub_${uriString.hashCode()}.$ext")
      contentResolver.openInputStream(uri)?.use { input ->
        FileOutputStream(outFile).use { output ->
          input.copyTo(output)
        }
      } ?: return null
      Uri.fromFile(outFile)
    }.getOrNull()
  }

  private fun queryDisplayName(uri: Uri): String? {
    return runCatching {
      contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
          val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
          if (idx >= 0) cursor.getString(idx) else null
        } else null
      }
    }.getOrNull()
  }

  private fun refreshTracksInternal() {
    val currentAudioId = runCatching { mediaPlayer.audioTrack }.getOrDefault(-1)
    val rawAudio = runCatching { mediaPlayer.audioTracks }.getOrNull().orEmpty()
    val audioList = rawAudio
      .filterNotNull()
      .filter { it.id != -1 }
      .mapIndexed { idx, desc ->
        PlayerTrackOption(
          id = desc.id,
          label = formatTrackLabel(desc.name, idx + 1),
          isSelected = desc.id == currentAudioId
        )
      }
    _audioTracks.value = audioList

    val currentSpuId = runCatching { mediaPlayer.spuTrack }.getOrDefault(-1)
    val rawSpu = runCatching { mediaPlayer.spuTracks }.getOrNull().orEmpty()
    val offOption = PlayerTrackOption(
      id = -1,
      label = context.getString(R.string.player_repeat_off),
      isSelected = currentSpuId == -1
    )
    val spuList = rawSpu
      .filterNotNull()
      .filter { it.id != -1 }
      .mapIndexed { idx, desc ->
        PlayerTrackOption(
          id = desc.id,
          label = formatTrackLabel(desc.name, idx + 1),
          isSelected = desc.id == currentSpuId
        )
      }
    _subtitleTracks.value = listOf(offOption) + spuList
  }

  private fun formatTrackLabel(rawName: String?, fallbackIndex: Int): String {
    val clean = rawName?.trim().orEmpty()
    if (clean.isBlank()) return "Track $fallbackIndex"
    val lower = clean.lowercase()
    return when {
      lower.contains("japanese") || lower == "jpn" || lower == "ja" || lower.contains("[jpn]") || lower.contains("[ja]") ->
        if (clean.length <= 5) context.getString(R.string.settings_audio_lang_jpn)
        else "$clean (${context.getString(R.string.settings_audio_lang_jpn)})"
      lower.contains("english") || lower == "eng" || lower == "en" || lower.contains("[eng]") || lower.contains("[en]") ->
        if (clean.length <= 7) context.getString(R.string.settings_audio_lang_eng)
        else "$clean (${context.getString(R.string.settings_audio_lang_eng)})"
      lower.contains("indonesian") || lower == "ind" || lower == "id" || lower.contains("[ind]") || lower.contains("[id]") ->
        if (clean.length <= 10) context.getString(R.string.settings_audio_lang_ind)
        else "$clean (${context.getString(R.string.settings_audio_lang_ind)})"
      else -> clean
    }
  }

  private fun refreshMediaInfoInternal() {
    val videoTrack = runCatching { mediaPlayer.currentVideoTrack }.getOrNull()
    var resolution = ""
    var videoCodec = ""
    var audioCodec = ""

    if (videoTrack != null) {
      if (videoTrack.width > 0 && videoTrack.height > 0) {
        resolution = "${videoTrack.width} × ${videoTrack.height}"
      }
      videoCodec = videoTrack.codec?.trim().orEmpty()
    }

    val media = runCatching { mediaPlayer.media }.getOrNull()
    if (media != null) {
      runCatching {
        for (i in 0 until media.trackCount) {
          val track = media.getTrack(i)
          when (track.type) {
            IMedia.Track.Type.Video -> {
              val vt = track as? IMedia.VideoTrack
              if (vt != null) {
                if (resolution.isBlank() && vt.width > 0 && vt.height > 0) {
                  resolution = "${vt.width} × ${vt.height}"
                }
                if (videoCodec.isBlank()) {
                  videoCodec = vt.codec?.trim().orEmpty()
                }
              }
            }
            IMedia.Track.Type.Audio -> {
              val at = track as? IMedia.AudioTrack
              if (at != null && audioCodec.isBlank()) {
                val codecName = at.codec?.trim().orEmpty().ifBlank { "?" }
                audioCodec = "$codecName • ${at.channels}ch • ${at.rate}Hz"
              }
            }
          }
        }
      }
      runCatching { media.release() }
    }

    _mediaInfo.value = VlcMediaInfo(
      resolution = resolution,
      videoCodec = videoCodec,
      audioCodec = audioCodec
    )
  }
}
