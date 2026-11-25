package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "animeplay_settings")

data class AppSettings(
  // Folder & MAL
  val scanFolders: Set<String> = emptySet(),
  val scanTreeUris: Set<String> = emptySet(),
  val autoEnrichMal: Boolean = true,

  // Onboarding & Celebrations
  val hasCompletedOnboarding: Boolean = false,
  val celebratedMilestones: Set<String> = emptySet(),

  // Playback basics
  val defaultPlaybackSpeed: Float = 1.0f,
  val autoPlayNextEpisode: Boolean = true,
  val autoEnableSubtitles: Boolean = true,
  val watchedThresholdPercent: Int = 90,

  // VLC Video & Hardware Acceleration
  val hardwareAccelerationMode: String = "auto", // auto, hardware, software
  val defaultAspectRatio: String = "fit", // fit, fill, zoom, 16_9, 4_3
  val playerOrientationMode: String = "sensor_landscape", // sensor_landscape, locked_landscape, system
  val enablePictureInPicture: Boolean = true,
  val keepScreenOn: Boolean = true,

  // VLC Audio & Equalizer
  val audioEqualizerPreset: String = "normal", // normal, anime, action, slice_of_life, ost, horror_mystery, mecha_scifi, bass, cinema, night
  val audioBoostPercent: Int = 100, // 100..200
  val skipSilence: Boolean = false,
  val preferredAudioLanguage: String = "jpn", // auto, jpn, eng, ind

  // VLC Subtitle Styling
  val subtitleFontSizeSp: Int = 18, // 14, 18, 22, 26
  val subtitleColorPreset: String = "white", // white, yellow, orange, cyan
  val subtitleBackgroundBox: Boolean = true,

  // VLC Controls, Gestures & Performance
  val seekJumpSeconds: Int = 10, // 5, 10, 15, 30
  val controlsAutoHideSeconds: Int = 3, // 2, 3, 5, 0 (manual)
  val enableBrightnessVolumeGestures: Boolean = true,
  val enableDoubleTapSeek: Boolean = true,
  val showClockAndBatteryInPlayer: Boolean = true,
  val skipIntroDurationSeconds: Int = 85, // 75, 85, 90
  val bufferProfile: String = "balanced", // low_ram, balanced, high
  val saveWatchHistory: Boolean = true,

  // Pemindai & Skip Intro/Outro + VLC Mobile Extras
  val autoScanIntroOutro: Boolean = true,
  val autoSkipIntro: Boolean = false,
  val autoSkipOutro: Boolean = false,
  val enableHorizontalSwipeSeek: Boolean = true,
  val enablePinchZoom: Boolean = true,
  val backgroundAudioPlay: Boolean = false,
  val nightDimmerPercent: Int = 0, // 0..60
  val defaultRepeatMode: String = "off", // off, one, all
  val subtitleDelayMs: Int = 0 // -5000..5000
)

@Singleton
class AppSettingsDataStore @Inject constructor(
  @ApplicationContext private val context: Context
) {

  private object Keys {
    val SCAN_FOLDERS = stringSetPreferencesKey("scan_folders")
    val SCAN_TREE_URIS = stringSetPreferencesKey("scan_tree_uris")
    val AUTO_ENRICH_MAL = booleanPreferencesKey("auto_enrich_mal")
    val HAS_COMPLETED_ONBOARDING = booleanPreferencesKey("has_completed_onboarding")
    val CELEBRATED_MILESTONES = stringSetPreferencesKey("celebrated_milestones")

    val DEFAULT_SPEED = floatPreferencesKey("default_playback_speed")
    val AUTO_PLAY_NEXT = booleanPreferencesKey("auto_play_next")
    val AUTO_SUBTITLES = booleanPreferencesKey("auto_enable_subtitles")
    val WATCHED_THRESHOLD = intPreferencesKey("watched_threshold_percent")

    val HW_ACCEL_MODE = stringPreferencesKey("hw_accel_mode")
    val DEFAULT_ASPECT_RATIO = stringPreferencesKey("default_aspect_ratio")
    val PLAYER_ORIENTATION = stringPreferencesKey("player_orientation")
    val ENABLE_PIP = booleanPreferencesKey("enable_pip")
    val KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")

    val AUDIO_EQ_PRESET = stringPreferencesKey("audio_eq_preset")
    val AUDIO_BOOST_PERCENT = intPreferencesKey("audio_boost_percent")
    val SKIP_SILENCE = booleanPreferencesKey("skip_silence")
    val PREFERRED_AUDIO_LANG = stringPreferencesKey("preferred_audio_lang")

    val SUBTITLE_FONT_SIZE = intPreferencesKey("subtitle_font_size_sp")
    val SUBTITLE_COLOR_PRESET = stringPreferencesKey("subtitle_color_preset")
    val SUBTITLE_BG_BOX = booleanPreferencesKey("subtitle_bg_box")

    val SEEK_JUMP_SECONDS = intPreferencesKey("seek_jump_seconds")
    val CONTROLS_AUTO_HIDE_SECONDS = intPreferencesKey("controls_auto_hide_seconds")
    val ENABLE_GESTURES = booleanPreferencesKey("enable_gestures")
    val ENABLE_DOUBLE_TAP = booleanPreferencesKey("enable_double_tap")
    val SHOW_CLOCK_BATTERY = booleanPreferencesKey("show_clock_battery")
    val SKIP_INTRO_SECONDS = intPreferencesKey("skip_intro_seconds")
    val BUFFER_PROFILE = stringPreferencesKey("buffer_profile")
    val SAVE_WATCH_HISTORY = booleanPreferencesKey("save_watch_history")

    val AUTO_SCAN_INTRO_OUTRO = booleanPreferencesKey("auto_scan_intro_outro")
    val AUTO_SKIP_INTRO = booleanPreferencesKey("auto_skip_intro")
    val AUTO_SKIP_OUTRO = booleanPreferencesKey("auto_skip_outro")
    val HORIZONTAL_SWIPE_SEEK = booleanPreferencesKey("horizontal_swipe_seek")
    val PINCH_ZOOM = booleanPreferencesKey("pinch_zoom")
    val BACKGROUND_AUDIO_PLAY = booleanPreferencesKey("background_audio_play")
    val NIGHT_DIMMER_PERCENT = intPreferencesKey("night_dimmer_percent")
    val DEFAULT_REPEAT_MODE = stringPreferencesKey("default_repeat_mode")
    val SUBTITLE_DELAY_MS = intPreferencesKey("subtitle_delay_ms")
  }

  val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { prefs ->
    AppSettings(
      scanFolders = prefs[Keys.SCAN_FOLDERS] ?: emptySet(),
      scanTreeUris = prefs[Keys.SCAN_TREE_URIS] ?: emptySet(),
      autoEnrichMal = prefs[Keys.AUTO_ENRICH_MAL] ?: true,
      hasCompletedOnboarding = prefs[Keys.HAS_COMPLETED_ONBOARDING] ?: false,
      celebratedMilestones = prefs[Keys.CELEBRATED_MILESTONES] ?: emptySet(),
      defaultPlaybackSpeed = prefs[Keys.DEFAULT_SPEED] ?: 1.0f,
      autoPlayNextEpisode = prefs[Keys.AUTO_PLAY_NEXT] ?: true,
      autoEnableSubtitles = prefs[Keys.AUTO_SUBTITLES] ?: true,
      watchedThresholdPercent = prefs[Keys.WATCHED_THRESHOLD] ?: 90,
      hardwareAccelerationMode = prefs[Keys.HW_ACCEL_MODE] ?: "auto",
      defaultAspectRatio = prefs[Keys.DEFAULT_ASPECT_RATIO] ?: "fit",
      playerOrientationMode = prefs[Keys.PLAYER_ORIENTATION] ?: "sensor_landscape",
      enablePictureInPicture = prefs[Keys.ENABLE_PIP] ?: true,
      keepScreenOn = prefs[Keys.KEEP_SCREEN_ON] ?: true,
      audioEqualizerPreset = prefs[Keys.AUDIO_EQ_PRESET] ?: "normal",
      audioBoostPercent = prefs[Keys.AUDIO_BOOST_PERCENT] ?: 100,
      skipSilence = prefs[Keys.SKIP_SILENCE] ?: false,
      preferredAudioLanguage = prefs[Keys.PREFERRED_AUDIO_LANG] ?: "jpn",
      subtitleFontSizeSp = prefs[Keys.SUBTITLE_FONT_SIZE] ?: 18,
      subtitleColorPreset = prefs[Keys.SUBTITLE_COLOR_PRESET] ?: "white",
      subtitleBackgroundBox = prefs[Keys.SUBTITLE_BG_BOX] ?: true,
      seekJumpSeconds = prefs[Keys.SEEK_JUMP_SECONDS] ?: 10,
      controlsAutoHideSeconds = prefs[Keys.CONTROLS_AUTO_HIDE_SECONDS] ?: 3,
      enableBrightnessVolumeGestures = prefs[Keys.ENABLE_GESTURES] ?: true,
      enableDoubleTapSeek = prefs[Keys.ENABLE_DOUBLE_TAP] ?: true,
      showClockAndBatteryInPlayer = prefs[Keys.SHOW_CLOCK_BATTERY] ?: true,
      skipIntroDurationSeconds = prefs[Keys.SKIP_INTRO_SECONDS] ?: 85,
      bufferProfile = prefs[Keys.BUFFER_PROFILE] ?: "balanced",
      saveWatchHistory = prefs[Keys.SAVE_WATCH_HISTORY] ?: true,
      autoScanIntroOutro = prefs[Keys.AUTO_SCAN_INTRO_OUTRO] ?: true,
      autoSkipIntro = prefs[Keys.AUTO_SKIP_INTRO] ?: false,
      autoSkipOutro = prefs[Keys.AUTO_SKIP_OUTRO] ?: false,
      enableHorizontalSwipeSeek = prefs[Keys.HORIZONTAL_SWIPE_SEEK] ?: true,
      enablePinchZoom = prefs[Keys.PINCH_ZOOM] ?: true,
      backgroundAudioPlay = prefs[Keys.BACKGROUND_AUDIO_PLAY] ?: false,
      nightDimmerPercent = prefs[Keys.NIGHT_DIMMER_PERCENT] ?: 0,
      defaultRepeatMode = prefs[Keys.DEFAULT_REPEAT_MODE] ?: "off",
      subtitleDelayMs = prefs[Keys.SUBTITLE_DELAY_MS] ?: 0
    )
  }

  suspend fun addScanFolder(relativePath: String, treeUri: String?) {
    val normalized = relativePath.trim().trim('/')
    context.dataStore.edit { prefs ->
      if (normalized.isNotBlank()) {
        val current = prefs[Keys.SCAN_FOLDERS] ?: emptySet()
        prefs[Keys.SCAN_FOLDERS] = current + normalized
      }
      if (!treeUri.isNullOrBlank()) {
        val currentTrees = prefs[Keys.SCAN_TREE_URIS] ?: emptySet()
        prefs[Keys.SCAN_TREE_URIS] = currentTrees + treeUri
      }
    }
  }

  suspend fun removeScanFolder(folderPath: String) {
    context.dataStore.edit { prefs ->
      val current = prefs[Keys.SCAN_FOLDERS] ?: emptySet()
      prefs[Keys.SCAN_FOLDERS] = current - folderPath
    }
  }

  suspend fun removeScanTreeUri(treeUri: String) {
    context.dataStore.edit { prefs ->
      val current = prefs[Keys.SCAN_TREE_URIS] ?: emptySet()
      prefs[Keys.SCAN_TREE_URIS] = current - treeUri
    }
  }

  suspend fun setOnboardingCompleted(completed: Boolean) {
    context.dataStore.edit { prefs -> prefs[Keys.HAS_COMPLETED_ONBOARDING] = completed }
  }

  suspend fun markMilestoneCelebrated(milestoneId: String) {
    context.dataStore.edit { prefs ->
      val current = prefs[Keys.CELEBRATED_MILESTONES] ?: emptySet()
      prefs[Keys.CELEBRATED_MILESTONES] = current + milestoneId
    }
  }

  suspend fun setAutoEnrichMal(enabled: Boolean) {
    context.dataStore.edit { prefs -> prefs[Keys.AUTO_ENRICH_MAL] = enabled }
  }

  suspend fun setDefaultPlaybackSpeed(speed: Float) {
    context.dataStore.edit { prefs -> prefs[Keys.DEFAULT_SPEED] = speed }
  }

  suspend fun setAutoPlayNextEpisode(enabled: Boolean) {
    context.dataStore.edit { prefs -> prefs[Keys.AUTO_PLAY_NEXT] = enabled }
  }

  suspend fun setAutoEnableSubtitles(enabled: Boolean) {
    context.dataStore.edit { prefs -> prefs[Keys.AUTO_SUBTITLES] = enabled }
  }

  suspend fun setWatchedThresholdPercent(percent: Int) {
    context.dataStore.edit { prefs -> prefs[Keys.WATCHED_THRESHOLD] = percent.coerceIn(70, 99) }
  }

  suspend fun setHardwareAccelerationMode(mode: String) {
    context.dataStore.edit { prefs -> prefs[Keys.HW_ACCEL_MODE] = mode }
  }

  suspend fun setDefaultAspectRatio(ratio: String) {
    context.dataStore.edit { prefs -> prefs[Keys.DEFAULT_ASPECT_RATIO] = ratio }
  }

  suspend fun setPlayerOrientationMode(mode: String) {
    context.dataStore.edit { prefs -> prefs[Keys.PLAYER_ORIENTATION] = mode }
  }

  suspend fun setEnablePictureInPicture(enabled: Boolean) {
    context.dataStore.edit { prefs -> prefs[Keys.ENABLE_PIP] = enabled }
  }

  suspend fun setKeepScreenOn(enabled: Boolean) {
    context.dataStore.edit { prefs -> prefs[Keys.KEEP_SCREEN_ON] = enabled }
  }

  suspend fun setAudioEqualizerPreset(preset: String) {
    context.dataStore.edit { prefs -> prefs[Keys.AUDIO_EQ_PRESET] = preset }
  }

  suspend fun setAudioBoostPercent(percent: Int) {
    context.dataStore.edit { prefs -> prefs[Keys.AUDIO_BOOST_PERCENT] = percent.coerceIn(100, 200) }
  }

  suspend fun setSkipSilence(enabled: Boolean) {
    context.dataStore.edit { prefs -> prefs[Keys.SKIP_SILENCE] = enabled }
  }

  suspend fun setPreferredAudioLanguage(lang: String) {
    context.dataStore.edit { prefs -> prefs[Keys.PREFERRED_AUDIO_LANG] = lang }
  }

  suspend fun setSubtitleFontSizeSp(sizeSp: Int) {
    context.dataStore.edit { prefs -> prefs[Keys.SUBTITLE_FONT_SIZE] = sizeSp.coerceIn(12, 32) }
  }

  suspend fun setSubtitleColorPreset(color: String) {
    context.dataStore.edit { prefs -> prefs[Keys.SUBTITLE_COLOR_PRESET] = color }
  }

  suspend fun setSubtitleBackgroundBox(enabled: Boolean) {
    context.dataStore.edit { prefs -> prefs[Keys.SUBTITLE_BG_BOX] = enabled }
  }

  suspend fun setSeekJumpSeconds(seconds: Int) {
    context.dataStore.edit { prefs -> prefs[Keys.SEEK_JUMP_SECONDS] = seconds.coerceIn(5, 60) }
  }

  suspend fun setControlsAutoHideSeconds(seconds: Int) {
    context.dataStore.edit { prefs -> prefs[Keys.CONTROLS_AUTO_HIDE_SECONDS] = seconds }
  }

  suspend fun setEnableBrightnessVolumeGestures(enabled: Boolean) {
    context.dataStore.edit { prefs -> prefs[Keys.ENABLE_GESTURES] = enabled }
  }

  suspend fun setEnableDoubleTapSeek(enabled: Boolean) {
    context.dataStore.edit { prefs -> prefs[Keys.ENABLE_DOUBLE_TAP] = enabled }
  }

  suspend fun setShowClockAndBatteryInPlayer(enabled: Boolean) {
    context.dataStore.edit { prefs -> prefs[Keys.SHOW_CLOCK_BATTERY] = enabled }
  }

  suspend fun setSkipIntroDurationSeconds(seconds: Int) {
    context.dataStore.edit { prefs -> prefs[Keys.SKIP_INTRO_SECONDS] = seconds }
  }

  suspend fun setBufferProfile(profile: String) {
    context.dataStore.edit { prefs -> prefs[Keys.BUFFER_PROFILE] = profile }
  }

  suspend fun setSaveWatchHistory(enabled: Boolean) {
    context.dataStore.edit { prefs -> prefs[Keys.SAVE_WATCH_HISTORY] = enabled }
  }

  suspend fun setAutoScanIntroOutro(enabled: Boolean) {
    context.dataStore.edit { prefs -> prefs[Keys.AUTO_SCAN_INTRO_OUTRO] = enabled }
  }

  suspend fun setAutoSkipIntro(enabled: Boolean) {
    context.dataStore.edit { prefs -> prefs[Keys.AUTO_SKIP_INTRO] = enabled }
  }

  suspend fun setAutoSkipOutro(enabled: Boolean) {
    context.dataStore.edit { prefs -> prefs[Keys.AUTO_SKIP_OUTRO] = enabled }
  }

  suspend fun setEnableHorizontalSwipeSeek(enabled: Boolean) {
    context.dataStore.edit { prefs -> prefs[Keys.HORIZONTAL_SWIPE_SEEK] = enabled }
  }

  suspend fun setEnablePinchZoom(enabled: Boolean) {
    context.dataStore.edit { prefs -> prefs[Keys.PINCH_ZOOM] = enabled }
  }

  suspend fun setBackgroundAudioPlay(enabled: Boolean) {
    context.dataStore.edit { prefs -> prefs[Keys.BACKGROUND_AUDIO_PLAY] = enabled }
  }

  suspend fun setNightDimmerPercent(percent: Int) {
    context.dataStore.edit { prefs -> prefs[Keys.NIGHT_DIMMER_PERCENT] = percent.coerceIn(0, 60) }
  }

  suspend fun setDefaultRepeatMode(mode: String) {
    context.dataStore.edit { prefs -> prefs[Keys.DEFAULT_REPEAT_MODE] = mode }
  }

  suspend fun setSubtitleDelayMs(delayMs: Int) {
    context.dataStore.edit { prefs -> prefs[Keys.SUBTITLE_DELAY_MS] = delayMs.coerceIn(-5000, 5000) }
  }

  suspend fun resetPlaybackDefaults() {
    context.dataStore.edit { prefs ->
      prefs[Keys.DEFAULT_SPEED] = 1.0f
      prefs[Keys.AUTO_PLAY_NEXT] = true
      prefs[Keys.AUTO_SUBTITLES] = true
      prefs[Keys.WATCHED_THRESHOLD] = 90
      prefs[Keys.HW_ACCEL_MODE] = "auto"
      prefs[Keys.DEFAULT_ASPECT_RATIO] = "fit"
      prefs[Keys.PLAYER_ORIENTATION] = "sensor_landscape"
      prefs[Keys.ENABLE_PIP] = true
      prefs[Keys.KEEP_SCREEN_ON] = true
      prefs[Keys.AUDIO_EQ_PRESET] = "normal"
      prefs[Keys.AUDIO_BOOST_PERCENT] = 100
      prefs[Keys.SKIP_SILENCE] = false
      prefs[Keys.PREFERRED_AUDIO_LANG] = "jpn"
      prefs[Keys.SUBTITLE_FONT_SIZE] = 18
      prefs[Keys.SUBTITLE_COLOR_PRESET] = "white"
      prefs[Keys.SUBTITLE_BG_BOX] = true
      prefs[Keys.SEEK_JUMP_SECONDS] = 10
      prefs[Keys.CONTROLS_AUTO_HIDE_SECONDS] = 3
      prefs[Keys.ENABLE_GESTURES] = true
      prefs[Keys.ENABLE_DOUBLE_TAP] = true
      prefs[Keys.SHOW_CLOCK_BATTERY] = true
      prefs[Keys.SKIP_INTRO_SECONDS] = 85
      prefs[Keys.BUFFER_PROFILE] = "balanced"
      prefs[Keys.SAVE_WATCH_HISTORY] = true
      prefs[Keys.AUTO_SCAN_INTRO_OUTRO] = true
      prefs[Keys.AUTO_SKIP_INTRO] = false
      prefs[Keys.AUTO_SKIP_OUTRO] = false
      prefs[Keys.HORIZONTAL_SWIPE_SEEK] = true
      prefs[Keys.PINCH_ZOOM] = true
      prefs[Keys.BACKGROUND_AUDIO_PLAY] = false
      prefs[Keys.NIGHT_DIMMER_PERCENT] = 0
      prefs[Keys.DEFAULT_REPEAT_MODE] = "off"
      prefs[Keys.SUBTITLE_DELAY_MS] = 0
    }
  }
}
