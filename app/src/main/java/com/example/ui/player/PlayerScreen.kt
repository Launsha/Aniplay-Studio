package com.example.ui.player

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.os.BatteryManager
import android.os.Build
import android.util.Rational
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.Audiotrack
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.BrightnessHigh
import androidx.compose.material.icons.rounded.ClosedCaption
import androidx.compose.material.icons.rounded.ClosedCaptionDisabled
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.Forward10
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.ui.theme.AnimePlayRoleShapes
import com.example.ui.theme.ObsidianBackground
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.OnPrimaryBlack
import com.example.ui.theme.SecondaryGray
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.VlcOrange
import com.example.util.TimeFormatters
import com.example.util.scaleOnPress
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.videolan.libvlc.util.VLCVideoLayout
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

private enum class GestureKind {
  BRIGHTNESS,
  VOLUME
}

private data class GestureIndicatorState(
  val kind: GestureKind,
  val percent: Int
)

private val speedOptions = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)

@Composable
fun PlayerScreen(
  onBack: () -> Unit,
  viewModel: PlayerViewModel = hiltViewModel()
) {
  val context = LocalContext.current
  val view = LocalView.current
  val lifecycleOwner = LocalLifecycleOwner.current
  val activity = context as? Activity
  val state by viewModel.uiState.collectAsStateWithLifecycle()
  val latestState by rememberUpdatedState(state)
  val controller = viewModel.controller

  val isPlaying by controller.isPlaying.collectAsStateWithLifecycle()
  val controllerPositionMs by controller.positionMs.collectAsStateWithLifecycle()
  val controllerDurationMs by controller.durationMs.collectAsStateWithLifecycle()
  val isBuffering by controller.isBuffering.collectAsStateWithLifecycle()
  val vlcError by controller.error.collectAsStateWithLifecycle()
  val audioTrackOptions by controller.audioTracks.collectAsStateWithLifecycle()
  val subTrackOptions by controller.subtitleTracks.collectAsStateWithLifecycle()
  val mediaInfo by controller.mediaInfo.collectAsStateWithLifecycle()
  val endReachedCount by controller.endReachedCount.collectAsStateWithLifecycle()

  val latestIsPlaying by rememberUpdatedState(isPlaying)
  val latestPositionMs by rememberUpdatedState(controllerPositionMs)

  val audioManager = remember {
    context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
  }
  val batteryManager = remember {
    context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
  }

  val coroutineScope = rememberCoroutineScope()
  var controlsVisible by remember { mutableStateOf(true) }
  var isScreenLocked by remember { mutableStateOf(false) }
  var interactionNonce by remember { mutableIntStateOf(0) }
  var hasEnded by remember { mutableStateOf(false) }
  var isUserSeeking by remember { mutableStateOf(false) }
  var seekPreviewMs by remember { mutableLongStateOf(0L) }
  var showSpeedMenu by remember { mutableStateOf(false) }
  var showTrackDialog by remember { mutableStateOf(false) }
  var showToolsDialog by remember { mutableStateOf(false) }
  var showIntroOutroDialog by remember { mutableStateOf(false) }
  var showJumpToTimeDialog by remember { mutableStateOf(false) }
  var showMediaInfoDialog by remember { mutableStateOf(false) }
  var showQueueDialog by remember { mutableStateOf(false) }
  var doubleTapFeedback by remember { mutableStateOf<Pair<Boolean, Int>?>(null) }
  var gestureIndicator by remember { mutableStateOf<GestureIndicatorState?>(null) }
  var feedbackToastText by remember { mutableStateOf<String?>(null) }
  var accumulatedWatchMs by remember { mutableLongStateOf(0L) }

  var pinchZoomScale by remember { mutableFloatStateOf(1f) }
  var horizontalSwipeDeltaMs by remember { mutableStateOf<Long?>(null) }
  var horizontalSwipeTargetMs by remember { mutableLongStateOf(0L) }
  var abPointAMs by remember { mutableStateOf<Long?>(null) }
  var abPointBMs by remember { mutableStateOf<Long?>(null) }
  var autoSkippedIntroForEpId by remember { mutableLongStateOf(-1L) }
  var autoSkippedOutroForEpId by remember { mutableLongStateOf(-1L) }

  var currentClockText by remember { mutableStateOf("") }
  var batteryPercent by remember { mutableIntStateOf(100) }

  var sleepSecondsLeft by remember { mutableStateOf<Int?>(null) }
  var sleepOnEpisodeEnd by remember { mutableStateOf(false) }

  val episode = state.currentEpisode
  val totalDurationMs = if (controllerDurationMs > 0L) {
    controllerDurationMs
  } else {
    episode?.durationMs?.coerceAtLeast(0L) ?: 0L
  }
  val latestDurationMs by rememberUpdatedState(totalDurationMs)

  // Orientation & Keep Screen On
  DisposableEffect(activity, view, state.orientationMode, state.keepScreenOn) {
    val window = activity?.window
    activity?.requestedOrientation = when (state.orientationMode) {
      "locked_landscape" -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
      "system" -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
      else -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
    }
    if (state.keepScreenOn) {
      window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    } else {
      window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    val controllerInsets = window?.let { WindowCompat.getInsetsController(it, view) }
    controllerInsets?.systemBarsBehavior =
      WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    controllerInsets?.hide(WindowInsetsCompat.Type.systemBars())

    onDispose {
      window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
      controllerInsets?.show(WindowInsetsCompat.Type.systemBars())
      activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }
  }

  // Save progress whenever playback pauses
  var wasPlayingBefore by remember { mutableStateOf(false) }
  LaunchedEffect(isPlaying) {
    if (isPlaying) {
      hasEnded = false
    } else if (wasPlayingBefore) {
      val delta = accumulatedWatchMs
      accumulatedWatchMs = 0L
      viewModel.saveProgress(
        positionMs = latestPositionMs,
        durationMs = latestDurationMs,
        watchedDeltaMs = delta
      )
    }
    wasPlayingBefore = isPlaying
  }

  // Save progress on lifecycle pause and dispose
  DisposableEffect(lifecycleOwner, controller) {
    val observer = LifecycleEventObserver { _, event ->
      if (event == Lifecycle.Event.ON_PAUSE) {
        val inPip = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N &&
          activity?.isInPictureInPictureMode == true
        if (!inPip && !latestState.backgroundAudioPlay) {
          controller.pause()
        }
        val delta = accumulatedWatchMs
        accumulatedWatchMs = 0L
        viewModel.saveProgress(
          positionMs = latestPositionMs,
          durationMs = latestDurationMs,
          watchedDeltaMs = delta
        )
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose {
      val delta = accumulatedWatchMs
      accumulatedWatchMs = 0L
      viewModel.saveProgress(
        positionMs = latestPositionMs,
        durationMs = latestDurationMs,
        watchedDeltaMs = delta
      )
      lifecycleOwner.lifecycle.removeObserver(observer)
    }
  }

  LaunchedEffect(episode?.id) {
    abPointAMs = null
    abPointBMs = null
    hasEnded = false
    autoSkippedIntroForEpId = -1L
    autoSkippedOutroForEpId = -1L
  }

  // Handle EndReached events from VlcPlayerController
  var handledEndCount by remember { mutableLongStateOf(0L) }
  LaunchedEffect(endReachedCount) {
    if (endReachedCount <= handledEndCount) return@LaunchedEffect
    handledEndCount = endReachedCount
    hasEnded = true
    val dur = latestDurationMs
    val delta = accumulatedWatchMs
    accumulatedWatchMs = 0L
    viewModel.saveProgress(dur, dur, watchedDeltaMs = delta)

    if (sleepOnEpisodeEnd) {
      sleepOnEpisodeEnd = false
      controlsVisible = true
      return@LaunchedEffect
    }

    val next = latestState.nextEpisode
    when (latestState.repeatMode) {
      "one" -> {
        hasEnded = false
        controller.replayFromStart()
      }
      "all" -> {
        if (next != null) {
          viewModel.loadEpisode(next.id)
        } else {
          val firstEp = latestState.allAnimeEpisodes.firstOrNull()
          if (firstEp != null) {
            viewModel.loadEpisode(firstEp.id, explicitStartPosMs = 0L)
          } else {
            controlsVisible = true
          }
        }
      }
      else -> {
        if (latestState.autoPlayNextEpisode && next != null) {
          viewModel.loadEpisode(next.id)
        } else {
          controlsVisible = true
        }
      }
    }
  }

  val autoSkippedIntroMsg = stringResource(R.string.player_auto_skipped_intro)
  val autoSkippedOutroMsg = stringResource(R.string.player_auto_skipped_outro)

  // A-B Repeat, Auto-Skip Intro/Outro, clock/battery, sleep timer countdown, and 5s progress save
  LaunchedEffect(controller, episode?.id) {
    var elapsedSaveMs = 0L
    var secondTickMs = 0L
    val clockFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    while (isActive) {
      delay(250L)
      val pos = controller.positionMs.value.coerceAtLeast(0L)
      val dur = latestDurationMs
      val playingNow = latestIsPlaying

      if (!isUserSeeking && playingNow) {
        val pointA = abPointAMs
        val pointB = abPointBMs
        if (pointA != null && pointB != null && pointB > pointA && pos >= pointB) {
          controller.seekTo(pointA)
        }

        val curEp = latestState.currentEpisode
        if (curEp != null) {
          if (latestState.autoSkipIntro &&
            autoSkippedIntroForEpId != curEp.id &&
            curEp.introStartMs >= 0L &&
            curEp.introEndMs > curEp.introStartMs + 5_000L &&
            pos in curEp.introStartMs until (curEp.introEndMs - 2_000L)
          ) {
            autoSkippedIntroForEpId = curEp.id
            controller.seekTo(curEp.introEndMs)
            feedbackToastText = autoSkippedIntroMsg
          } else if (latestState.autoSkipOutro &&
            autoSkippedOutroForEpId != curEp.id &&
            curEp.outroStartMs >= 0L &&
            curEp.outroEndMs > curEp.outroStartMs + 5_000L &&
            pos in curEp.outroStartMs until (curEp.outroEndMs - 2_000L)
          ) {
            autoSkippedOutroForEpId = curEp.id
            val nextEp = latestState.nextEpisode
            if (nextEp != null && latestState.autoPlayNextEpisode) {
              viewModel.loadEpisode(nextEp.id)
            } else {
              controller.seekTo(curEp.outroEndMs)
            }
            feedbackToastText = autoSkippedOutroMsg
          }
        }
      }

      if (playingNow) {
        accumulatedWatchMs += 250L
        elapsedSaveMs += 250L
        if (elapsedSaveMs >= 5000L) {
          elapsedSaveMs = 0L
          val delta = accumulatedWatchMs
          accumulatedWatchMs = 0L
          viewModel.saveProgress(
            positionMs = pos,
            durationMs = dur,
            watchedDeltaMs = delta
          )
        }
      }

      secondTickMs += 250L
      if (secondTickMs >= 1000L) {
        secondTickMs = 0L
        currentClockText = clockFormat.format(Date())
        batteryPercent = batteryManager
          ?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
          ?.coerceIn(0, 100) ?: 100

        sleepSecondsLeft?.let { rem ->
          if (rem <= 1) {
            sleepSecondsLeft = null
            controller.pause()
            controlsVisible = true
          } else {
            sleepSecondsLeft = rem - 1
          }
        }
      }
    }
  }

  val hasPlaybackError = vlcError != null || state.errorMessage != null

  LaunchedEffect(
    controlsVisible,
    isPlaying,
    interactionNonce,
    showSpeedMenu,
    showTrackDialog,
    showToolsDialog,
    showIntroOutroDialog,
    showJumpToTimeDialog,
    showMediaInfoDialog,
    showQueueDialog,
    isScreenLocked,
    hasPlaybackError
  ) {
    val autoHideSec = state.controlsAutoHideSeconds
    if (controlsVisible &&
      isPlaying &&
      !showSpeedMenu &&
      !showTrackDialog &&
      !showToolsDialog &&
      !showIntroOutroDialog &&
      !showJumpToTimeDialog &&
      !showMediaInfoDialog &&
      !showQueueDialog &&
      !hasPlaybackError &&
      autoHideSec > 0
    ) {
      delay(autoHideSec * 1000L)
      controlsVisible = false
    }
  }

  LaunchedEffect(doubleTapFeedback) {
    if (doubleTapFeedback != null) {
      delay(650L)
      doubleTapFeedback = null
    }
  }

  LaunchedEffect(feedbackToastText) {
    if (feedbackToastText != null) {
      delay(1800L)
      feedbackToastText = null
    }
  }

  val subtitlePickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument()
  ) { uri ->
    if (uri != null) {
      runCatching {
        context.contentResolver.takePersistableUriPermission(
          uri,
          Intent.FLAG_GRANT_READ_URI_PERMISSION
        )
      }
      viewModel.onSubtitlePicked(uri.toString())
    }
  }

  val exitPlayer: () -> Unit = {
    val delta = accumulatedWatchMs
    accumulatedWatchMs = 0L
    viewModel.saveProgress(
      positionMs = controller.positionMs.value,
      durationMs = totalDurationMs,
      watchedDeltaMs = delta
    )
    onBack()
  }

  BackHandler {
    if (isScreenLocked) {
      isScreenLocked = false
      controlsVisible = true
    } else {
      exitPlayer()
    }
  }

  val aspectLabel = when (state.aspectRatioMode) {
    "fill" -> stringResource(R.string.settings_aspect_fill)
    "zoom" -> stringResource(R.string.settings_aspect_zoom)
    "16_9" -> stringResource(R.string.settings_aspect_16_9)
    "4_3" -> stringResource(R.string.settings_aspect_4_3)
    else -> stringResource(R.string.settings_aspect_fit)
  }

  BoxWithConstraints(
    modifier = Modifier
      .fillMaxSize()
      .background(Color.Black)
      .testTag("player_root")
  ) {
    val containerWidthPx = constraints.maxWidth.toFloat().coerceAtLeast(1f)
    val containerHeightPx = constraints.maxHeight.toFloat().coerceAtLeast(1f)

    AndroidView(
      factory = { ctx ->
        VLCVideoLayout(ctx).apply {
          keepScreenOn = state.keepScreenOn
          controller.attachView(this)
        }
      },
      update = { vlcLayout ->
        vlcLayout.keepScreenOn = state.keepScreenOn
        controller.attachView(vlcLayout)
        controller.setAspectRatioMode(state.aspectRatioMode)
      },
      onRelease = { vlcLayout ->
        controller.detachView(vlcLayout)
      },
      modifier = Modifier
        .fillMaxSize()
        .graphicsLayer(
          scaleX = pinchZoomScale,
          scaleY = pinchZoomScale
        )
    )

    NightDimmerLayer(dimmerPercent = state.nightDimmerPercent)

    var dragBrightness by remember { mutableFloatStateOf(0.5f) }
    var dragVolume by remember { mutableFloatStateOf(0.5f) }
    var activeGestureKind by remember { mutableStateOf<GestureKind?>(null) }
    var horizontalStartPosMs by remember { mutableLongStateOf(0L) }
    var horizontalAccumulatedPx by remember { mutableFloatStateOf(0f) }

    Box(
      modifier = Modifier
        .fillMaxSize()
        .pointerInput(isScreenLocked, state.enablePinchZoom) {
          if (!state.enablePinchZoom) return@pointerInput
          awaitEachGesture {
            var zoomed = false
            do {
              val event = awaitPointerEvent()
              if (!isScreenLocked && event.changes.count { it.pressed } >= 2) {
                val zoomChange = event.calculateZoom()
                if (zoomChange != 1f) {
                  pinchZoomScale = (pinchZoomScale * zoomChange).coerceIn(1f, 2.5f)
                  zoomed = true
                  event.changes.forEach { it.consume() }
                }
              }
            } while (event.changes.any { it.pressed })
            if (zoomed) {
              feedbackToastText = context.getString(R.string.player_zoom_badge, (pinchZoomScale * 100f).roundToInt())
            }
          }
        }
        .pointerInput(controller, isScreenLocked, state.enableDoubleTapSeek, state.seekJumpSeconds) {
          detectTapGestures(
            onTap = {
              controlsVisible = !controlsVisible
              interactionNonce++
            },
            onDoubleTap = { offset ->
              if (!isScreenLocked && state.enableDoubleTapSeek) {
                val isForward = offset.x >= containerWidthPx / 2f
                val jumpMs = state.seekJumpSeconds * 1000L
                val delta = if (isForward) jumpMs else -jumpMs
                val maxDur = latestDurationMs.takeIf { it > 0L } ?: Long.MAX_VALUE
                val target = (controller.positionMs.value + delta).coerceIn(0L, maxDur)
                controller.seekTo(target)
                doubleTapFeedback = isForward to ((doubleTapFeedback?.second ?: 0) + 1)
                interactionNonce++
              }
            }
          )
        }
        .pointerInput(activity, audioManager, isScreenLocked, state.enableBrightnessVolumeGestures) {
          detectVerticalDragGestures(
            onDragStart = { startOffset ->
              if (isScreenLocked || !state.enableBrightnessVolumeGestures) return@detectVerticalDragGestures
              val isLeft = startOffset.x < containerWidthPx / 2f
              activeGestureKind = if (isLeft) GestureKind.BRIGHTNESS else GestureKind.VOLUME
              if (isLeft) {
                val currentB = activity?.window?.attributes?.screenBrightness ?: -1f
                dragBrightness = if (currentB in 0f..1f) currentB else 0.5f
                gestureIndicator = GestureIndicatorState(
                  GestureKind.BRIGHTNESS,
                  (dragBrightness * 100f).roundToInt().coerceIn(1, 100)
                )
              } else {
                val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
                val curVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                dragVolume = (curVol.toFloat() / maxVol.toFloat()).coerceIn(0f, 1f)
                gestureIndicator = GestureIndicatorState(
                  GestureKind.VOLUME,
                  (dragVolume * 100f).roundToInt().coerceIn(0, 100)
                )
              }
            },
            onDragEnd = {
              activeGestureKind = null
              gestureIndicator = null
            },
            onDragCancel = {
              activeGestureKind = null
              gestureIndicator = null
            },
            onVerticalDrag = { _, dragAmount ->
              if (isScreenLocked || !state.enableBrightnessVolumeGestures) return@detectVerticalDragGestures
              val deltaFraction = -dragAmount / (containerHeightPx * 0.7f)
              when (activeGestureKind) {
                GestureKind.BRIGHTNESS -> {
                  dragBrightness = (dragBrightness + deltaFraction).coerceIn(0.01f, 1.0f)
                  activity?.window?.let { win ->
                    val lp = win.attributes
                    lp.screenBrightness = dragBrightness
                    win.attributes = lp
                  }
                  gestureIndicator = GestureIndicatorState(
                    GestureKind.BRIGHTNESS,
                    (dragBrightness * 100f).roundToInt().coerceIn(1, 100)
                  )
                }
                GestureKind.VOLUME -> {
                  dragVolume = (dragVolume + deltaFraction).coerceIn(0f, 1f)
                  val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
                  val targetVol = (dragVolume * maxVol).roundToInt().coerceIn(0, maxVol)
                  audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVol, 0)
                  gestureIndicator = GestureIndicatorState(
                    GestureKind.VOLUME,
                    (dragVolume * 100f).roundToInt().coerceIn(0, 100)
                  )
                }
                null -> Unit
              }
            }
          )
        }
        .pointerInput(controller, isScreenLocked, state.enableHorizontalSwipeSeek) {
          detectHorizontalDragGestures(
            onDragStart = {
              if (isScreenLocked || !state.enableHorizontalSwipeSeek) return@detectHorizontalDragGestures
              horizontalStartPosMs = controller.positionMs.value.coerceAtLeast(0L)
              horizontalAccumulatedPx = 0f
              horizontalSwipeDeltaMs = 0L
              horizontalSwipeTargetMs = horizontalStartPosMs
            },
            onDragEnd = {
              if (horizontalSwipeDeltaMs != null && !isScreenLocked && state.enableHorizontalSwipeSeek) {
                controller.seekTo(horizontalSwipeTargetMs)
                interactionNonce++
              }
              horizontalSwipeDeltaMs = null
            },
            onDragCancel = {
              horizontalSwipeDeltaMs = null
            },
            onHorizontalDrag = { _, dragAmount ->
              if (isScreenLocked || !state.enableHorizontalSwipeSeek) return@detectHorizontalDragGestures
              horizontalAccumulatedPx += dragAmount
              val deltaMs = ((horizontalAccumulatedPx / containerWidthPx) * 90_000f).toLong()
              val maxDur = latestDurationMs.takeIf { it > 0L } ?: Long.MAX_VALUE
              val target = (horizontalStartPosMs + deltaMs).coerceIn(0L, maxDur)
              horizontalSwipeTargetMs = target
              horizontalSwipeDeltaMs = target - horizontalStartPosMs
            }
          )
        }
    )

    HorizontalSwipeSeekOverlay(
      deltaMs = horizontalSwipeDeltaMs,
      targetPositionMs = horizontalSwipeTargetMs
    )

    AbRepeatActiveBadge(
      pointAMs = abPointAMs,
      pointBMs = abPointBMs,
      onClear = {
        abPointAMs = null
        abPointBMs = null
      }
    )

    val inIntroWindow = episode != null &&
      episode.introStartMs >= 0L &&
      episode.introEndMs > episode.introStartMs + 5_000L &&
      controllerPositionMs in episode.introStartMs until (episode.introEndMs - 1_000L)

    val inOutroWindow = episode != null &&
      episode.outroStartMs >= 0L &&
      episode.outroEndMs > episode.outroStartMs + 5_000L &&
      controllerPositionMs in episode.outroStartMs until (episode.outroEndMs - 1_000L)

    FloatingSkipIntroOutroPills(
      inIntro = inIntroWindow,
      inOutro = inOutroWindow,
      hasNextEpisode = state.nextEpisode != null,
      controlsVisible = controlsVisible,
      isScreenLocked = isScreenLocked,
      onSkipIntro = {
        val target = episode?.introEndMs ?: return@FloatingSkipIntroOutroPills
        controller.seekTo(target)
        interactionNonce++
      },
      onSkipOutro = {
        val nextEp = state.nextEpisode
        if (nextEp != null) {
          val delta = accumulatedWatchMs
          accumulatedWatchMs = 0L
          viewModel.saveProgress(
            controller.positionMs.value,
            totalDurationMs,
            watchedDeltaMs = delta
          )
          viewModel.loadEpisode(nextEp.id)
        } else {
          val target = episode?.outroEndMs ?: return@FloatingSkipIntroOutroPills
          controller.seekTo(target)
        }
        interactionNonce++
      }
    )

    doubleTapFeedback?.let { (isForward, _) ->
      Box(
        modifier = Modifier
          .fillMaxHeight()
          .fillMaxWidth(0.5f)
          .align(if (isForward) Alignment.CenterEnd else Alignment.CenterStart),
        contentAlignment = Alignment.Center
      ) {
        Surface(
          color = ObsidianSurface.copy(alpha = 0.88f),
          shape = AnimePlayRoleShapes.Chip,
          border = BorderStroke(1.dp, ObsidianBorder)
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Icon(
              imageVector = if (isForward) Icons.Rounded.Forward10 else Icons.Rounded.Replay10,
              contentDescription = null,
              tint = TextPrimary
            )
            Text(
              text = stringResource(
                if (isForward) R.string.player_seek_forward_badge else R.string.player_seek_backward_badge,
                state.seekJumpSeconds
              ),
              style = MaterialTheme.typography.titleSmall,
              color = TextPrimary
            )
          }
        }
      }
    }

    gestureIndicator?.let { indicator ->
      Surface(
        color = ObsidianSurface.copy(alpha = 0.9f),
        shape = AnimePlayRoleShapes.Dialog,
        border = BorderStroke(1.dp, ObsidianBorder),
        modifier = Modifier
          .align(Alignment.Center)
          .width(200.dp)
          .testTag("gesture_indicator")
      ) {
        Column(
          modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Icon(
              imageVector = if (indicator.kind == GestureKind.BRIGHTNESS) {
                Icons.Rounded.BrightnessHigh
              } else {
                Icons.AutoMirrored.Rounded.VolumeUp
              },
              contentDescription = stringResource(
                if (indicator.kind == GestureKind.BRIGHTNESS) {
                  R.string.cd_brightness_icon
                } else {
                  R.string.cd_volume_icon
                }
              ),
              tint = TextPrimary
            )
            Text(
              text = stringResource(
                if (indicator.kind == GestureKind.BRIGHTNESS) {
                  R.string.player_brightness_label
                } else {
                  R.string.player_volume_label
                },
                indicator.percent
              ),
              style = MaterialTheme.typography.labelLarge,
              color = TextPrimary
            )
          }
          LinearProgressIndicator(
            progress = { indicator.percent / 100f },
            color = VlcOrange,
            trackColor = ObsidianBackground,
            modifier = Modifier
              .fillMaxWidth()
              .height(6.dp)
              .clip(RoundedCornerShape(3.dp))
          )
        }
      }
    }

    feedbackToastText?.let { toastText ->
      Surface(
        color = ObsidianSurface.copy(alpha = 0.92f),
        shape = AnimePlayRoleShapes.Chip,
        border = BorderStroke(1.dp, ObsidianBorder),
        modifier = Modifier
          .align(Alignment.TopCenter)
          .windowInsetsPadding(WindowInsets.safeDrawing)
          .padding(top = 64.dp)
      ) {
        Text(
          text = toastText,
          style = MaterialTheme.typography.labelMedium,
          color = TextPrimary,
          modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
      }
    }

    if ((isBuffering || state.isLoadingEpisode) && !hasPlaybackError) {
      Column(
        modifier = Modifier.align(Alignment.Center),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        CircularProgressIndicator(color = VlcOrange, strokeWidth = 3.dp)
        Text(
          text = stringResource(R.string.player_loading),
          style = MaterialTheme.typography.bodySmall,
          color = TextPrimary
        )
      }
    }

    if (hasPlaybackError) {
      PlayerErrorOverlay(
        onRetry = {
          controller.clearError()
          viewModel.retryPlayback()
        },
        onBack = exitPlayer
      )
    }

    // Screen Locked Overlay (only Unlock button)
    AnimatedVisibility(
      visible = controlsVisible && isScreenLocked && !hasPlaybackError,
      enter = fadeIn(),
      exit = fadeOut(),
      modifier = Modifier
        .align(Alignment.CenterStart)
        .windowInsetsPadding(WindowInsets.safeDrawing)
        .padding(16.dp)
    ) {
      Surface(
        onClick = {
          isScreenLocked = false
          controlsVisible = true
          interactionNonce++
        },
        color = ObsidianSurface.copy(alpha = 0.9f),
        shape = AnimePlayRoleShapes.Button,
        border = BorderStroke(1.dp, ObsidianBorder)
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Icon(
            imageVector = Icons.Rounded.LockOpen,
            contentDescription = stringResource(R.string.player_unlock_screen),
            tint = TextPrimary
          )
          Text(
            text = stringResource(R.string.player_screen_locked_hint),
            style = MaterialTheme.typography.labelMedium,
            color = TextPrimary
          )
        }
      }
    }

    // Full VLC Controls Overlay
    AnimatedVisibility(
      visible = controlsVisible && !isScreenLocked && !hasPlaybackError,
      enter = fadeIn(),
      exit = fadeOut(),
      modifier = Modifier.fillMaxSize()
    ) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(Color.Black.copy(alpha = 0.48f))
          .windowInsetsPadding(WindowInsets.safeDrawing)
          .padding(horizontal = 16.dp, vertical = 10.dp)
      ) {
        // Top Bar
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .align(Alignment.TopCenter),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            IconButton(
              onClick = exitPlayer,
              modifier = Modifier
                .minimumInteractiveComponentSize()
                .testTag("player_back_button")
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = stringResource(R.string.action_back),
                tint = TextPrimary
              )
            }
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = if (episode != null) {
                  stringResource(R.string.player_episode_title, state.animeTitle, episode.number)
                } else {
                  state.animeTitle
                },
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
              Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                if (state.subtitleDisplayName != null) {
                  Text(
                    text = stringResource(
                      R.string.player_subtitle_loaded,
                      state.subtitleDisplayName.orEmpty()
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = SecondaryGray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                  )
                }
                if (state.showClockAndBattery) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                  ) {
                    Text(
                      text = currentClockText,
                      style = MaterialTheme.typography.labelSmall,
                      color = SecondaryGray
                    )
                    Icon(
                      imageVector = Icons.Rounded.BatteryChargingFull,
                      contentDescription = null,
                      tint = SecondaryGray,
                      modifier = Modifier.size(13.dp)
                    )
                    Text(
                      text = stringResource(R.string.player_battery_label, batteryPercent),
                      style = MaterialTheme.typography.labelSmall,
                      color = SecondaryGray
                    )
                  }
                }
                sleepSecondsLeft?.let { remSec ->
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                  ) {
                    Icon(
                      imageVector = Icons.Rounded.Bedtime,
                      contentDescription = null,
                      tint = TextPrimary,
                      modifier = Modifier.size(13.dp)
                    )
                    Text(
                      text = stringResource(
                        R.string.player_sleep_remaining,
                        TimeFormatters.formatDuration(remSec * 1000L)
                      ),
                      style = MaterialTheme.typography.labelSmall,
                      color = TextPrimary
                    )
                  }
                }
              }
            }
          }

          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
          ) {
            IconButton(
              onClick = {
                viewModel.toggleSubtitles()
                interactionNonce++
              },
              modifier = Modifier
                .minimumInteractiveComponentSize()
                .testTag("player_subtitle_toggle")
            ) {
              Icon(
                imageVector = if (state.subtitlesEnabled) {
                  Icons.Rounded.ClosedCaption
                } else {
                  Icons.Rounded.ClosedCaptionDisabled
                },
                contentDescription = stringResource(
                  if (state.subtitlesEnabled) R.string.player_subtitle_on else R.string.player_subtitle_off
                ),
                tint = if (state.subtitlesEnabled) VlcOrange else SecondaryGray
              )
            }

            IconButton(
              onClick = {
                showQueueDialog = true
                interactionNonce++
              },
              modifier = Modifier
                .minimumInteractiveComponentSize()
                .testTag("player_queue_button")
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Rounded.PlaylistPlay,
                contentDescription = stringResource(R.string.player_queue_title),
                tint = TextPrimary
              )
            }

            IconButton(
              onClick = {
                showTrackDialog = true
                interactionNonce++
              },
              modifier = Modifier
                .minimumInteractiveComponentSize()
                .testTag("player_tracks_button")
            ) {
              Icon(
                imageVector = Icons.Rounded.Audiotrack,
                contentDescription = stringResource(R.string.player_tracks_title),
                tint = TextPrimary
              )
            }

            Box {
              TextButton(
                onClick = {
                  showSpeedMenu = true
                  interactionNonce++
                },
                shape = AnimePlayRoleShapes.Button,
                modifier = Modifier
                  .minimumInteractiveComponentSize()
                  .testTag("player_speed_button")
              ) {
                Icon(
                  imageVector = Icons.Rounded.Speed,
                  contentDescription = stringResource(R.string.player_speed_label),
                  tint = TextPrimary,
                  modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = stringResource(R.string.player_speed_value, formatSpeed(state.playbackSpeed)),
                  style = MaterialTheme.typography.labelLarge,
                  color = TextPrimary
                )
              }

              DropdownMenu(
                expanded = showSpeedMenu,
                onDismissRequest = { showSpeedMenu = false },
                containerColor = ObsidianSurface
              ) {
                speedOptions.forEach { speed ->
                  DropdownMenuItem(
                    text = {
                      Text(
                        text = stringResource(R.string.player_speed_value, formatSpeed(speed)),
                        color = if (speed == state.playbackSpeed) VlcOrange else TextPrimary
                      )
                    },
                    onClick = {
                      viewModel.setPlaybackSpeed(speed)
                      showSpeedMenu = false
                      interactionNonce++
                    }
                  )
                }
              }
            }

            IconButton(
              onClick = {
                showToolsDialog = true
                interactionNonce++
              },
              modifier = Modifier
                .minimumInteractiveComponentSize()
                .testTag("player_tools_button")
            ) {
              Icon(
                imageVector = Icons.Rounded.Tune,
                contentDescription = stringResource(R.string.player_more_tools),
                tint = TextPrimary
              )
            }
          }
        }

        // Center Transport Controls
        Row(
          modifier = Modifier.align(Alignment.Center),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(32.dp)
        ) {
          IconButton(
            onClick = {
              val target = (controller.positionMs.value - state.seekJumpSeconds * 1000L).coerceAtLeast(0L)
              controller.seekTo(target)
              interactionNonce++
            },
            modifier = Modifier
              .size(56.dp)
              .clip(CircleShape)
              .background(ObsidianSurface.copy(alpha = 0.72f))
              .testTag("player_rewind_button")
          ) {
            Icon(
              imageVector = Icons.Rounded.Replay10,
              contentDescription = stringResource(R.string.player_rewind_10, state.seekJumpSeconds),
              tint = TextPrimary,
              modifier = Modifier.size(30.dp)
            )
          }

          val playInteraction = remember { MutableInteractionSource() }
          Surface(
            onClick = {
              if (isPlaying) {
                controller.pause()
              } else if (hasEnded) {
                hasEnded = false
                controller.replayFromStart()
              } else {
                controller.play()
              }
              interactionNonce++
            },
            interactionSource = playInteraction,
            color = VlcOrange,
            shape = CircleShape,
            modifier = Modifier
              .size(68.dp)
              .scaleOnPress(playInteraction)
              .testTag("player_play_pause_button")
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = stringResource(
                  if (isPlaying) R.string.player_pause else R.string.player_play
                ),
                tint = OnPrimaryBlack,
                modifier = Modifier.size(38.dp)
              )
            }
          }

          IconButton(
            onClick = {
              val maxDur = totalDurationMs.takeIf { it > 0L } ?: Long.MAX_VALUE
              val target = (controller.positionMs.value + state.seekJumpSeconds * 1000L).coerceIn(0L, maxDur)
              controller.seekTo(target)
              interactionNonce++
            },
            modifier = Modifier
              .size(56.dp)
              .clip(CircleShape)
              .background(ObsidianSurface.copy(alpha = 0.72f))
              .testTag("player_forward_button")
          ) {
            Icon(
              imageVector = Icons.Rounded.Forward10,
              contentDescription = stringResource(R.string.player_forward_10, state.seekJumpSeconds),
              tint = TextPrimary,
              modifier = Modifier.size(30.dp)
            )
          }
        }

        // Bottom Seekbar + VLC Action Strip
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .align(Alignment.BottomCenter),
          verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          val displayedPosition = if (isUserSeeking) seekPreviewMs else controllerPositionMs
          val safeDuration = totalDurationMs.coerceAtLeast(1L)

          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Text(
              text = TimeFormatters.formatDuration(displayedPosition),
              style = MaterialTheme.typography.labelMedium,
              color = TextPrimary
            )

            Slider(
              value = displayedPosition.toFloat().coerceIn(0f, safeDuration.toFloat()),
              onValueChange = { value ->
                isUserSeeking = true
                seekPreviewMs = value.toLong()
                interactionNonce++
              },
              onValueChangeFinished = {
                controller.seekTo(seekPreviewMs)
                isUserSeeking = false
                viewModel.saveProgress(seekPreviewMs, totalDurationMs, watchedDeltaMs = 0L)
                interactionNonce++
              },
              valueRange = 0f..safeDuration.toFloat(),
              colors = SliderDefaults.colors(
                thumbColor = VlcOrange,
                activeTrackColor = VlcOrange,
                inactiveTrackColor = SecondaryGray.copy(alpha = 0.35f)
              ),
              modifier = Modifier
                .weight(1f)
                .testTag("player_seek_slider")
            )

            Text(
              text = TimeFormatters.formatDuration(totalDurationMs),
              style = MaterialTheme.typography.labelMedium,
              color = SecondaryGray
            )
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              IconButton(
                onClick = {
                  isScreenLocked = true
                  controlsVisible = true
                },
                modifier = Modifier
                  .minimumInteractiveComponentSize()
                  .testTag("player_lock_button")
              ) {
                Icon(
                  imageVector = Icons.Rounded.Lock,
                  contentDescription = stringResource(R.string.player_lock_screen),
                  tint = TextPrimary
                )
              }

              TextButton(
                onClick = {
                  viewModel.cycleAspectRatio()
                  interactionNonce++
                },
                modifier = Modifier
                  .minimumInteractiveComponentSize()
                  .testTag("player_aspect_button")
              ) {
                Icon(
                  imageVector = Icons.Rounded.AspectRatio,
                  contentDescription = stringResource(R.string.player_aspect_ratio),
                  tint = TextPrimary,
                  modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = aspectLabel,
                  style = MaterialTheme.typography.labelSmall,
                  color = TextPrimary
                )
              }

              OutlinedButton(
                onClick = {
                  val maxDur = totalDurationMs.takeIf { it > 0L } ?: Long.MAX_VALUE
                  val target = (controller.positionMs.value + state.skipIntroDurationSeconds * 1000L)
                    .coerceIn(0L, maxDur)
                  controller.seekTo(target)
                  interactionNonce++
                },
                shape = AnimePlayRoleShapes.Button,
                border = BorderStroke(1.dp, ObsidianBorder),
                modifier = Modifier
                  .height(38.dp)
                  .testTag("player_skip_op_button")
              ) {
                Icon(
                  imageVector = Icons.Rounded.FastForward,
                  contentDescription = null,
                  tint = TextPrimary,
                  modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = stringResource(R.string.player_skip_op, state.skipIntroDurationSeconds),
                  style = MaterialTheme.typography.labelSmall,
                  color = TextPrimary
                )
              }

              OutlinedButton(
                onClick = {
                  showIntroOutroDialog = true
                  interactionNonce++
                },
                shape = AnimePlayRoleShapes.Button,
                border = BorderStroke(1.dp, ObsidianBorder),
                modifier = Modifier
                  .height(38.dp)
                  .testTag("player_intro_outro_button")
              ) {
                Icon(
                  imageVector = Icons.Rounded.AutoFixHigh,
                  contentDescription = null,
                  tint = TextPrimary,
                  modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = stringResource(R.string.player_intro_outro_button),
                  style = MaterialTheme.typography.labelSmall,
                  color = TextPrimary
                )
              }
            }

            val nextEp = state.nextEpisode
            if (nextEp != null) {
              val nextInteraction = remember { MutableInteractionSource() }
              Button(
                onClick = {
                  val delta = accumulatedWatchMs
                  accumulatedWatchMs = 0L
                  viewModel.saveProgress(
                    controller.positionMs.value,
                    totalDurationMs,
                    watchedDeltaMs = delta
                  )
                  viewModel.loadEpisode(nextEp.id)
                  interactionNonce++
                },
                interactionSource = nextInteraction,
                colors = ButtonDefaults.buttonColors(
                  containerColor = ObsidianSurface.copy(alpha = 0.9f),
                  contentColor = TextPrimary
                ),
                shape = AnimePlayRoleShapes.Button,
                border = BorderStroke(1.dp, ObsidianBorder),
                modifier = Modifier
                  .height(44.dp)
                  .scaleOnPress(nextInteraction)
                  .testTag("player_next_episode_button")
              ) {
                Icon(
                  imageVector = Icons.Rounded.SkipNext,
                  contentDescription = null,
                  modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = "${stringResource(R.string.player_next_episode)} (${stringResource(R.string.detail_episode_label, nextEp.number)})",
                  style = MaterialTheme.typography.labelMedium
                )
              }
            }
          }
        }
      }
    }
  }

  if (showTrackDialog) {
    TrackSelectorDialog(
      audioTracks = audioTrackOptions,
      subtitleTracks = subTrackOptions,
      onSelectAudioTrack = { opt ->
        viewModel.selectAudioTrack(opt.id)
      },
      onSelectSubtitleTrack = { opt ->
        viewModel.selectSubtitleTrack(opt.id)
      },
      onPickExternalSubtitle = {
        subtitlePickerLauncher.launch(
          arrayOf(
            "application/x-subrip",
            "text/plain",
            "text/x-ssa",
            "application/octet-stream",
            "*/*"
          )
        )
      },
      onDismiss = { showTrackDialog = false }
    )
  }

  if (showToolsDialog) {
    val bookmarkSavedFmt = stringResource(R.string.player_bookmark_saved)
    val abPointAToastFmt = stringResource(R.string.player_ab_point_a_toast)
    val screenshotSavedMsg = stringResource(R.string.player_screenshot_saved)
    val screenshotFailedMsg = stringResource(R.string.player_screenshot_failed)
    VlcToolsDialog(
      eqPreset = state.audioEqualizerPreset,
      audioBoostPercent = state.audioBoostPercent,
      sleepMinutesRemaining = sleepSecondsLeft?.let { ((it + 59) / 60).coerceAtLeast(1) },
      sleepOnEpisodeEnd = sleepOnEpisodeEnd,
      repeatMode = state.repeatMode,
      abPointAMs = abPointAMs,
      abPointBMs = abPointBMs,
      subtitleDelayMs = state.subtitleDelayMs,
      nightDimmerPercent = state.nightDimmerPercent,
      autoSkipIntro = state.autoSkipIntro,
      autoSkipOutro = state.autoSkipOutro,
      backgroundAudioPlay = state.backgroundAudioPlay,
      canUsePip = state.enablePictureInPicture && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O,
      onSelectEqPreset = viewModel::setEqualizerPreset,
      onChangeAudioBoost = viewModel::setAudioBoostPercent,
      onSelectSleepMinutes = { mins ->
        sleepOnEpisodeEnd = false
        sleepSecondsLeft = mins?.times(60)
      },
      onSelectSleepEndOfEpisode = {
        sleepSecondsLeft = null
        sleepOnEpisodeEnd = true
      },
      onSelectRepeatMode = viewModel::setRepeatMode,
      onSetAbPointA = {
        val pos = controller.positionMs.value.coerceAtLeast(0L)
        abPointAMs = pos
        if (abPointBMs != null && abPointBMs!! <= pos) abPointBMs = null
        feedbackToastText = String.format(abPointAToastFmt, TimeFormatters.formatDuration(pos))
      },
      onSetAbPointB = {
        val pos = controller.positionMs.value.coerceAtLeast(0L)
        val a = abPointAMs ?: (pos - 10_000L).coerceAtLeast(0L)
        abPointAMs = a
        abPointBMs = pos.coerceAtLeast(a + 2_000L)
      },
      onClearAbRepeat = {
        abPointAMs = null
        abPointBMs = null
      },
      onChangeSubtitleDelay = viewModel::setSubtitleDelayMs,
      onChangeNightDimmer = viewModel::setNightDimmerPercent,
      onToggleAutoSkipIntro = viewModel::setAutoSkipIntro,
      onToggleAutoSkipOutro = viewModel::setAutoSkipOutro,
      onToggleBackgroundAudio = viewModel::setBackgroundAudioPlay,
      onOpenIntroOutroScanner = { showIntroOutroDialog = true },
      onOpenJumpToTime = { showJumpToTimeDialog = true },
      onOpenMediaInfo = { showMediaInfoDialog = true },
      onCaptureScreenshot = {
        val pos = controller.positionMs.value.coerceAtLeast(0L)
        coroutineScope.launch {
          val ok = viewModel.captureFrameToPictures(pos)
          feedbackToastText = if (ok) screenshotSavedMsg else screenshotFailedMsg
        }
      },
      onAddBookmark = {
        val pos = controller.positionMs.value.coerceAtLeast(0L)
        val formatted = TimeFormatters.formatDuration(pos)
        viewModel.addBookmarkAt(pos, "${state.animeTitle} ($formatted)")
        feedbackToastText = String.format(bookmarkSavedFmt, formatted)
      },
      onEnterPip = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
          runCatching {
            activity?.enterPictureInPictureMode(
              PictureInPictureParams.Builder()
                .setAspectRatio(Rational(16, 9))
                .build()
            )
          }
        }
      },
      onDismiss = { showToolsDialog = false }
    )
  }

  if (showIntroOutroDialog) {
    val foundFmt = stringResource(R.string.intro_outro_scan_found)
    val noneMsg = stringResource(R.string.intro_outro_scan_none)
    val savedMsg = stringResource(R.string.intro_outro_saved)
    IntroOutroScannerDialog(
      episode = episode,
      currentPositionMs = controllerPositionMs,
      isScanning = state.isScanningIntroOutro,
      scanMessage = state.introOutroScanMessage,
      onRunAutoScan = { viewModel.scanCurrentEpisodeIntroOutro(foundFmt, noneMsg) },
      onSaveSegments = { iS, iE, oS, oE, src, all ->
        viewModel.saveIntroOutroSegments(iS, iE, oS, oE, src, all)
        feedbackToastText = savedMsg
      },
      onDismiss = { showIntroOutroDialog = false }
    )
  }

  if (showJumpToTimeDialog) {
    JumpToTimeDialog(
      currentPositionMs = controllerPositionMs,
      totalDurationMs = totalDurationMs,
      onJump = { targetMs ->
        controller.seekTo(targetMs)
      },
      onDismiss = { showJumpToTimeDialog = false }
    )
  }

  if (showMediaInfoDialog) {
    val unknown = stringResource(R.string.player_media_unknown)
    MediaInfoDialog(
      fileName = episode?.fileName ?: unknown,
      resolution = mediaInfo.resolution.ifBlank { unknown },
      videoCodec = mediaInfo.videoCodec.ifBlank { unknown },
      audioCodec = mediaInfo.audioCodec.ifBlank { unknown },
      durationMs = totalDurationMs,
      onDismiss = { showMediaInfoDialog = false }
    )
  }

  if (showQueueDialog) {
    PlayerEpisodeQueueDialog(
      episodes = state.allAnimeEpisodes,
      currentEpisodeId = episode?.id ?: -1L,
      onSelectEpisode = { chosen ->
        val delta = accumulatedWatchMs
        accumulatedWatchMs = 0L
        viewModel.saveProgress(
          controller.positionMs.value,
          totalDurationMs,
          watchedDeltaMs = delta
        )
        viewModel.loadEpisode(chosen.id)
      },
      onDismiss = { showQueueDialog = false }
    )
  }
}

@Composable
private fun PlayerErrorOverlay(
  onRetry: () -> Unit,
  onBack: () -> Unit
) {
  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(ObsidianBackground.copy(alpha = 0.94f))
      .padding(24.dp),
    contentAlignment = Alignment.Center
  ) {
    Column(
      modifier = Modifier.widthIn(max = 420.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      Icon(
        imageVector = Icons.Rounded.ErrorOutline,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.error,
        modifier = Modifier.size(48.dp)
      )
      Text(
        text = stringResource(R.string.player_error_title),
        style = MaterialTheme.typography.titleLarge,
        color = TextPrimary,
        textAlign = TextAlign.Center
      )
      Text(
        text = stringResource(R.string.player_error_description),
        style = MaterialTheme.typography.bodyMedium,
        color = SecondaryGray,
        textAlign = TextAlign.Center
      )
      Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        OutlinedButton(
          onClick = onBack,
          shape = AnimePlayRoleShapes.Button,
          border = BorderStroke(1.dp, ObsidianBorder),
          modifier = Modifier.height(48.dp)
        ) {
          Text(
            text = stringResource(R.string.player_back_to_detail),
            color = TextPrimary
          )
        }
        Button(
          onClick = onRetry,
          shape = AnimePlayRoleShapes.Button,
          colors = ButtonDefaults.buttonColors(
            containerColor = TextPrimary,
            contentColor = OnPrimaryBlack
          ),
          modifier = Modifier.height(48.dp)
        ) {
          Text(text = stringResource(R.string.action_retry))
        }
      }
    }
  }
}

private fun formatSpeed(speed: Float): String {
  return if (speed % 1f == 0f) {
    "${speed.toInt()}.0"
  } else {
    speed.toString()
  }
}
