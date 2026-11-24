package com.example.ui.player

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.net.Uri
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.BrightnessHigh
import androidx.compose.material.icons.rounded.ClosedCaption
import androidx.compose.material.icons.rounded.ClosedCaptionDisabled
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Forward10
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Subtitles
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.R
import com.example.ui.theme.ObsidianBackground
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.OnPrimaryBlack
import com.example.ui.theme.SecondaryGray
import com.example.ui.theme.VlcOrange
import com.example.util.TimeFormatters
import com.example.util.scaleOnPress
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
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

@OptIn(UnstableApi::class)
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

  val audioManager = remember {
    context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
  }

  var controlsVisible by remember { mutableStateOf(true) }
  var interactionNonce by remember { mutableIntStateOf(0) }
  var isPlaying by remember { mutableStateOf(false) }
  var isBuffering by remember { mutableStateOf(true) }
  var playbackError by remember { mutableStateOf(false) }
  var currentPositionMs by remember { mutableLongStateOf(0L) }
  var totalDurationMs by remember { mutableLongStateOf(0L) }
  var isUserSeeking by remember { mutableStateOf(false) }
  var seekPreviewMs by remember { mutableLongStateOf(0L) }
  var showSpeedMenu by remember { mutableStateOf(false) }
  var doubleTapFeedback by remember { mutableStateOf<Pair<Boolean, Int>?>(null) } // isForward, nonce
  var gestureIndicator by remember { mutableStateOf<GestureIndicatorState?>(null) }

  // Lightweight ExoPlayer configured for 2GB RAM target devices
  val exoPlayer = remember(context) {
    val loadControl = DefaultLoadControl.Builder()
      .setBufferDurationsMs(10_000, 30_000, 1_500, 3_000)
      .build()

    val audioAttrs = AudioAttributes.Builder()
      .setUsage(C.USAGE_MEDIA)
      .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
      .build()

    ExoPlayer.Builder(context)
      .setLoadControl(loadControl)
      .setAudioAttributes(audioAttrs, true)
      .build()
  }

  // Force landscape, immersive system bars, and keep screen on while playing video
  DisposableEffect(activity, view) {
    val window = activity?.window
    activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
    window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

    val controller = window?.let { WindowCompat.getInsetsController(it, view) }
    controller?.systemBarsBehavior =
      WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    controller?.hide(WindowInsetsCompat.Type.systemBars())

    onDispose {
      window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
      controller?.show(WindowInsetsCompat.Type.systemBars())
      activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }
  }

  // Handle ExoPlayer listener & release on exit
  DisposableEffect(exoPlayer) {
    val listener = object : Player.Listener {
      override fun onIsPlayingChanged(playing: Boolean) {
        isPlaying = playing
        if (!playing) {
          viewModel.saveProgress(exoPlayer.currentPosition, exoPlayer.duration.coerceAtLeast(0L))
        }
      }

      override fun onPlaybackStateChanged(playbackState: Int) {
        isBuffering = playbackState == Player.STATE_BUFFERING
        if (playbackState == Player.STATE_READY) {
          playbackError = false
          val dur = exoPlayer.duration.coerceAtLeast(0L)
          if (dur > 0L) totalDurationMs = dur
        } else if (playbackState == Player.STATE_ENDED) {
          val dur = exoPlayer.duration.coerceAtLeast(0L)
          viewModel.saveProgress(dur, dur)
          controlsVisible = true
        }
      }

      override fun onPlayerError(error: PlaybackException) {
        isBuffering = false
        playbackError = true
        controlsVisible = true
      }
    }
    exoPlayer.addListener(listener)
    onDispose {
      viewModel.saveProgress(exoPlayer.currentPosition, exoPlayer.duration.coerceAtLeast(0L))
      exoPlayer.removeListener(listener)
      exoPlayer.release()
    }
  }

  // Pause and persist position when lifecycle pauses
  DisposableEffect(lifecycleOwner, exoPlayer) {
    val observer = LifecycleEventObserver { _, event ->
      if (event == Lifecycle.Event.ON_PAUSE) {
        exoPlayer.pause()
        viewModel.saveProgress(exoPlayer.currentPosition, exoPlayer.duration.coerceAtLeast(0L))
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose {
      lifecycleOwner.lifecycle.removeObserver(observer)
    }
  }

  // Load or reload MediaItem when episode or external subtitle changes
  val episode = state.currentEpisode
  LaunchedEffect(episode?.id, state.subtitleUri, state.subtitleMimeType) {
    if (episode != null) {
      playbackError = false
      isBuffering = true

      val resumePosition = if (exoPlayer.currentMediaItem?.mediaId == episode.id.toString()) {
        exoPlayer.currentPosition.coerceAtLeast(0L)
      } else {
        val saved = episode.lastPositionMs
        val dur = episode.durationMs
        if (saved > 0L && !episode.isWatched && (dur <= 0L || saved < (dur * 0.90).toLong())) {
          saved
        } else {
          0L
        }
      }

      val mediaItemBuilder = MediaItem.Builder()
        .setMediaId(episode.id.toString())
        .setUri(Uri.parse(episode.fileUri))

      val subUri = state.subtitleUri
      if (!subUri.isNullOrBlank()) {
        val subConfig = MediaItem.SubtitleConfiguration.Builder(Uri.parse(subUri))
          .setMimeType(state.subtitleMimeType)
          .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
          .build()
        mediaItemBuilder.setSubtitleConfigurations(listOf(subConfig))
      }

      exoPlayer.setMediaItem(mediaItemBuilder.build(), resumePosition)
      exoPlayer.playbackParameters = PlaybackParameters(state.playbackSpeed)
      exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
        .buildUpon()
        .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, !state.subtitlesEnabled)
        .build()
      exoPlayer.prepare()
      exoPlayer.playWhenReady = true

      totalDurationMs = episode.durationMs.coerceAtLeast(0L)
      currentPositionMs = resumePosition
    }
  }

  // Apply subtitle toggle without reloading media
  LaunchedEffect(state.subtitlesEnabled) {
    exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
      .buildUpon()
      .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, !state.subtitlesEnabled)
      .build()
  }

  // Apply playback speed
  LaunchedEffect(state.playbackSpeed) {
    exoPlayer.playbackParameters = PlaybackParameters(state.playbackSpeed)
  }

  // Update UI seekbar every 250ms and save progress to Room every 5 seconds while playing
  LaunchedEffect(exoPlayer, episode?.id) {
    var elapsedSaveMs = 0L
    while (isActive) {
      delay(250L)
      if (!isUserSeeking && exoPlayer.playbackState != Player.STATE_IDLE) {
        val pos = exoPlayer.currentPosition.coerceAtLeast(0L)
        val dur = exoPlayer.duration.coerceAtLeast(0L)
        currentPositionMs = pos
        if (dur > 0L) totalDurationMs = dur
      }
      if (exoPlayer.isPlaying) {
        elapsedSaveMs += 250L
        if (elapsedSaveMs >= 5000L) {
          elapsedSaveMs = 0L
          viewModel.saveProgress(
            positionMs = exoPlayer.currentPosition,
            durationMs = exoPlayer.duration.coerceAtLeast(0L)
          )
        }
      }
    }
  }

  // Auto-hide controls after 3 seconds when playing
  LaunchedEffect(controlsVisible, isPlaying, interactionNonce, showSpeedMenu) {
    if (controlsVisible && isPlaying && !showSpeedMenu && !playbackError) {
      delay(3000L)
      controlsVisible = false
    }
  }

  // Auto-clear double tap feedback badge
  LaunchedEffect(doubleTapFeedback) {
    if (doubleTapFeedback != null) {
      delay(650L)
      doubleTapFeedback = null
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

  BackHandler {
    viewModel.saveProgress(exoPlayer.currentPosition, exoPlayer.duration.coerceAtLeast(0L))
    onBack()
  }

  val exitPlayer: () -> Unit = {
    viewModel.saveProgress(exoPlayer.currentPosition, exoPlayer.duration.coerceAtLeast(0L))
    onBack()
  }

  BoxWithConstraints(
    modifier = Modifier
      .fillMaxSize()
      .background(Color.Black)
      .testTag("player_root")
  ) {
    val containerWidthPx = constraints.maxWidth.toFloat().coerceAtLeast(1f)
    val containerHeightPx = constraints.maxHeight.toFloat().coerceAtLeast(1f)

    // Media3 PlayerView surface (custom controls disabled; SubtitleView enabled)
    AndroidView(
      factory = { ctx ->
        PlayerView(ctx).apply {
          useController = false
          keepScreenOn = true
          player = exoPlayer
        }
      },
      update = { playerView ->
        if (playerView.player !== exoPlayer) {
          playerView.player = exoPlayer
        }
      },
      modifier = Modifier.fillMaxSize()
    )

    // Gesture layer: single tap (toggle controls), double tap (+/-10s), vertical swipe (brightness/volume)
    var dragBrightness by remember { mutableFloatStateOf(0.5f) }
    var dragVolume by remember { mutableFloatStateOf(0.5f) }
    var activeGestureKind by remember { mutableStateOf<GestureKind?>(null) }

    Box(
      modifier = Modifier
        .fillMaxSize()
        .pointerInput(exoPlayer) {
          detectTapGestures(
            onTap = {
              controlsVisible = !controlsVisible
              interactionNonce++
            },
            onDoubleTap = { offset ->
              val isForward = offset.x >= containerWidthPx / 2f
              val delta = if (isForward) 10_000L else -10_000L
              val maxDur = exoPlayer.duration.takeIf { it > 0L } ?: Long.MAX_VALUE
              val target = (exoPlayer.currentPosition + delta).coerceIn(0L, maxDur)
              exoPlayer.seekTo(target)
              currentPositionMs = target
              doubleTapFeedback = isForward to ((doubleTapFeedback?.second ?: 0) + 1)
              interactionNonce++
            }
          )
        }
        .pointerInput(activity, audioManager) {
          detectVerticalDragGestures(
            onDragStart = { startOffset ->
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
    )

    // Double-tap seek visual badge
    doubleTapFeedback?.let { (isForward, _) ->
      Box(
        modifier = Modifier
          .fillMaxHeight()
          .fillMaxWidth(0.5f)
          .align(if (isForward) Alignment.CenterEnd else Alignment.CenterStart),
        contentAlignment = Alignment.Center
      ) {
        Surface(
          color = ObsidianBackground.copy(alpha = 0.75f),
          shape = RoundedCornerShape(50),
          modifier = Modifier.border(1.dp, VlcOrange.copy(alpha = 0.6f), RoundedCornerShape(50))
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Icon(
              imageVector = if (isForward) Icons.Rounded.Forward10 else Icons.Rounded.Replay10,
              contentDescription = null,
              tint = VlcOrange
            )
            Text(
              text = stringResource(
                if (isForward) R.string.player_seek_forward_badge else R.string.player_seek_backward_badge
              ),
              style = MaterialTheme.typography.titleSmall,
              color = MaterialTheme.colorScheme.onBackground
            )
          }
        }
      }
    }

    // Brightness / Volume gesture indicator
    gestureIndicator?.let { indicator ->
      Surface(
        color = ObsidianBackground.copy(alpha = 0.85f),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
          .align(Alignment.Center)
          .width(200.dp)
          .border(1.dp, ObsidianBorder, RoundedCornerShape(16.dp))
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
              tint = VlcOrange
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
              color = MaterialTheme.colorScheme.onBackground
            )
          }
          LinearProgressIndicator(
            progress = { indicator.percent / 100f },
            color = VlcOrange,
            trackColor = ObsidianSurface,
            modifier = Modifier
              .fillMaxWidth()
              .height(6.dp)
              .clip(RoundedCornerShape(3.dp))
          )
        }
      }
    }

    // Loading state
    if ((isBuffering || state.isLoadingEpisode) && !playbackError && state.errorMessage == null) {
      Column(
        modifier = Modifier.align(Alignment.Center),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        CircularProgressIndicator(color = VlcOrange, strokeWidth = 3.dp)
        Text(
          text = stringResource(R.string.player_loading),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onBackground
        )
      }
    }

    // Error state (corrupt or missing file)
    if (playbackError || state.errorMessage != null) {
      PlayerErrorOverlay(
        onRetry = {
          playbackError = false
          episode?.id?.let { viewModel.loadEpisode(it) }
          exoPlayer.prepare()
          exoPlayer.playWhenReady = true
        },
        onBack = exitPlayer
      )
    }

    // Custom VLC + Netflix Compose controls overlay
    AnimatedVisibility(
      visible = controlsVisible && !playbackError && state.errorMessage == null,
      enter = fadeIn(),
      exit = fadeOut(),
      modifier = Modifier.fillMaxSize()
    ) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(
            Brush.verticalGradient(
              colors = listOf(
                ObsidianBackground.copy(alpha = 0.78f),
                Color.Black.copy(alpha = 0.25f),
                ObsidianBackground.copy(alpha = 0.86f)
              )
            )
          )
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
                tint = MaterialTheme.colorScheme.onBackground
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
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
              if (state.subtitleDisplayName != null) {
                Text(
                  text = stringResource(
                    R.string.player_subtitle_loaded,
                    state.subtitleDisplayName.orEmpty()
                  ),
                  style = MaterialTheme.typography.labelSmall,
                  color = VlcOrange,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
              }
            }
          }

          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            // Toggle subtitles On/Off
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

            // Pick subtitle fallback button (SAF)
            TextButton(
              onClick = {
                interactionNonce++
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
              modifier = Modifier
                .minimumInteractiveComponentSize()
                .testTag("player_select_subtitle_button")
            ) {
              Icon(
                imageVector = Icons.Rounded.Subtitles,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(18.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = stringResource(R.string.player_select_subtitle),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onBackground
              )
            }

            // Speed selector (0.5x - 2.0x)
            Box {
              TextButton(
                onClick = {
                  showSpeedMenu = true
                  interactionNonce++
                },
                modifier = Modifier
                  .minimumInteractiveComponentSize()
                  .testTag("player_speed_button")
              ) {
                Icon(
                  imageVector = Icons.Rounded.Speed,
                  contentDescription = stringResource(R.string.player_speed_label),
                  tint = VlcOrange,
                  modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = stringResource(R.string.player_speed_value, formatSpeed(state.playbackSpeed)),
                  style = MaterialTheme.typography.labelLarge,
                  color = VlcOrange
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
                        color = if (speed == state.playbackSpeed) VlcOrange else MaterialTheme.colorScheme.onSurface
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
          }
        }

        // Center Transport Controls (-10s, Play/Pause, +10s)
        Row(
          modifier = Modifier.align(Alignment.Center),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(32.dp)
        ) {
          IconButton(
            onClick = {
              val target = (exoPlayer.currentPosition - 10_000L).coerceAtLeast(0L)
              exoPlayer.seekTo(target)
              currentPositionMs = target
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
              contentDescription = stringResource(R.string.player_rewind_10),
              tint = MaterialTheme.colorScheme.onBackground,
              modifier = Modifier.size(30.dp)
            )
          }

          val playInteraction = remember { MutableInteractionSource() }
          Surface(
            onClick = {
              if (exoPlayer.isPlaying) {
                exoPlayer.pause()
              } else {
                if (exoPlayer.playbackState == Player.STATE_ENDED) {
                  exoPlayer.seekTo(0L)
                }
                exoPlayer.play()
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
              val maxDur = exoPlayer.duration.takeIf { it > 0L } ?: Long.MAX_VALUE
              val target = (exoPlayer.currentPosition + 10_000L).coerceIn(0L, maxDur)
              exoPlayer.seekTo(target)
              currentPositionMs = target
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
              contentDescription = stringResource(R.string.player_forward_10),
              tint = MaterialTheme.colorScheme.onBackground,
              modifier = Modifier.size(30.dp)
            )
          }
        }

        // Bottom SeekBar & Next Episode
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .align(Alignment.BottomCenter),
          verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
          val displayedPosition = if (isUserSeeking) seekPreviewMs else currentPositionMs
          val safeDuration = totalDurationMs.coerceAtLeast(1L)

          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Text(
              text = TimeFormatters.formatDuration(displayedPosition),
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.onBackground
            )

            Slider(
              value = displayedPosition.toFloat().coerceIn(0f, safeDuration.toFloat()),
              onValueChange = { value ->
                isUserSeeking = true
                seekPreviewMs = value.toLong()
                interactionNonce++
              },
              onValueChangeFinished = {
                exoPlayer.seekTo(seekPreviewMs)
                currentPositionMs = seekPreviewMs
                isUserSeeking = false
                viewModel.saveProgress(seekPreviewMs, totalDurationMs)
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

          val nextEp = state.nextEpisode
          if (nextEp != null) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.End
            ) {
              val nextInteraction = remember { MutableInteractionSource() }
              Button(
                onClick = {
                  viewModel.saveProgress(
                    exoPlayer.currentPosition,
                    exoPlayer.duration.coerceAtLeast(0L)
                  )
                  viewModel.loadEpisode(nextEp.id)
                  interactionNonce++
                },
                interactionSource = nextInteraction,
                colors = ButtonDefaults.buttonColors(
                  containerColor = ObsidianSurface.copy(alpha = 0.9f),
                  contentColor = VlcOrange
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                  .height(48.dp)
                  .scaleOnPress(nextInteraction)
                  .border(1.dp, VlcOrange.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                  .testTag("player_next_episode_button")
              ) {
                Icon(
                  imageVector = Icons.Rounded.SkipNext,
                  contentDescription = null,
                  modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = "${stringResource(R.string.player_next_episode)} (Ep ${nextEp.number})",
                  style = MaterialTheme.typography.labelLarge
                )
              }
            }
          }
        }
      }
    }
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
        color = MaterialTheme.colorScheme.onBackground,
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
          modifier = Modifier.height(48.dp)
        ) {
          Text(
            text = stringResource(R.string.player_back_to_detail),
            color = SecondaryGray
          )
        }
        Button(
          onClick = onRetry,
          colors = ButtonDefaults.buttonColors(
            containerColor = VlcOrange,
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
