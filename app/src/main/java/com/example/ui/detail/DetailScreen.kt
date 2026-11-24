package com.example.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.local.entity.EpisodeEntity
import com.example.ui.components.VideoThumbnail
import com.example.ui.theme.ObsidianBackground
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceElevated
import com.example.ui.theme.OnPrimaryBlack
import com.example.ui.theme.SecondaryGray
import com.example.ui.theme.VlcOrange
import com.example.ui.theme.VlcOrangeSoft
import com.example.util.TimeFormatters
import com.example.util.scaleOnPress

@Composable
fun DetailScreen(
  onBack: () -> Unit,
  onEpisodeClick: (Long) -> Unit,
  viewModel: DetailViewModel = hiltViewModel()
) {
  val state by viewModel.uiState.collectAsStateWithLifecycle()
  var showEditDialog by remember { mutableStateOf(false) }

  val anime = state.anime

  Scaffold(
    containerColor = ObsidianBackground,
    contentWindowInsets = WindowInsets(0)
  ) { innerPadding ->
    LazyColumn(
      contentPadding = PaddingValues(bottom = 28.dp),
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .windowInsetsPadding(WindowInsets.navigationBars)
        .testTag("detail_episode_list")
    ) {
      item {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(280.dp)
        ) {
          VideoThumbnail(
            uri = anime?.coverUri,
            contentDescription = stringResource(R.string.cd_anime_thumbnail, anime?.title.orEmpty()),
            modifier = Modifier.fillMaxSize(),
            targetWidthPx = 720,
            targetHeightPx = 420
          )
          Box(
            modifier = Modifier
              .fillMaxSize()
              .background(
                Brush.verticalGradient(
                  colors = listOf(
                    ObsidianBackground.copy(alpha = 0.55f),
                    ObsidianBackground.copy(alpha = 0.25f),
                    ObsidianBackground
                  )
                )
              )
          )

          IconButton(
            onClick = onBack,
            modifier = Modifier
              .windowInsetsPadding(WindowInsets.statusBars)
              .padding(12.dp)
              .size(48.dp)
              .clip(CircleShape)
              .background(ObsidianBackground.copy(alpha = 0.65f))
              .testTag("detail_back_button")
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
              contentDescription = stringResource(R.string.action_back),
              tint = MaterialTheme.colorScheme.onBackground
            )
          }

          Column(
            modifier = Modifier
              .align(Alignment.BottomStart)
              .fillMaxWidth()
              .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Text(
              text = anime?.title.orEmpty(),
              style = MaterialTheme.typography.headlineMedium,
              color = MaterialTheme.colorScheme.onBackground,
              maxLines = 2,
              overflow = TextOverflow.Ellipsis
            )

            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              val watchedCount = state.episodes.count { it.isWatched }
              MetaBadge(text = stringResource(R.string.home_episode_count, state.episodes.size))
              MetaBadge(text = stringResource(R.string.home_watched_progress, watchedCount, state.episodes.size))
              anime?.let {
                MetaBadge(
                  text = buildString {
                    append(it.genre)
                    it.releaseYear?.let { year -> append(" • $year") }
                  }
                )
              }
              Surface(
                color = ObsidianSurfaceElevated.copy(alpha = 0.9f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                  .clip(RoundedCornerShape(8.dp))
                  .clickable { showEditDialog = true }
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                  Icon(
                    imageVector = Icons.Rounded.Edit,
                    contentDescription = stringResource(R.string.detail_edit_info),
                    tint = VlcOrange,
                    modifier = Modifier.size(13.dp)
                  )
                  Text(
                    text = stringResource(R.string.detail_edit_info),
                    style = MaterialTheme.typography.labelSmall,
                    color = VlcOrange
                  )
                }
              }
            }
          }
        }
      }

      item {
        val nextEp = state.nextToPlayEpisode
        if (nextEp != null) {
          val playInteraction = remember { MutableInteractionSource() }
          Button(
            onClick = { onEpisodeClick(nextEp.id) },
            interactionSource = playInteraction,
            colors = ButtonDefaults.buttonColors(
              containerColor = VlcOrange,
              contentColor = OnPrimaryBlack
            ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 16.dp, vertical = 8.dp)
              .height(50.dp)
              .scaleOnPress(playInteraction)
              .testTag("detail_primary_play_button")
          ) {
            Icon(
              imageVector = Icons.Rounded.PlayArrow,
              contentDescription = null,
              modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = if (nextEp.lastPositionMs > 0L && !nextEp.isWatched) {
                stringResource(R.string.detail_continue_episode, nextEp.number)
              } else {
                stringResource(R.string.detail_play_now)
              },
              style = MaterialTheme.typography.labelLarge
            )
          }
        }

        Text(
          text = stringResource(R.string.detail_episode_list_title),
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onBackground,
          modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 8.dp)
        )
      }

      if (state.episodes.isEmpty()) {
        item {
          Text(
            text = stringResource(R.string.detail_no_episodes),
            style = MaterialTheme.typography.bodyMedium,
            color = SecondaryGray,
            modifier = Modifier.padding(16.dp)
          )
        }
      } else {
        items(state.episodes, key = { it.id }) { episode ->
          EpisodeRowItem(
            episode = episode,
            onPlayClick = { onEpisodeClick(episode.id) },
            onToggleWatched = { viewModel.toggleEpisodeWatched(episode) }
          )
        }
      }
    }
  }

  if (showEditDialog && anime != null) {
    EditMetadataDialog(
      initialGenre = anime.genre,
      initialYear = anime.releaseYear?.toString().orEmpty(),
      onDismiss = { showEditDialog = false },
      onSave = { genre, year ->
        viewModel.updateAnimeMetadata(genre, year)
        showEditDialog = false
      }
    )
  }
}

@Composable
private fun MetaBadge(text: String) {
  Surface(
    color = ObsidianSurface.copy(alpha = 0.88f),
    shape = RoundedCornerShape(8.dp),
    modifier = Modifier.border(1.dp, ObsidianBorder, RoundedCornerShape(8.dp))
  ) {
    Text(
      text = text,
      style = MaterialTheme.typography.labelSmall,
      color = MaterialTheme.colorScheme.onSurface,
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
    )
  }
}

@Composable
private fun EpisodeRowItem(
  episode: EpisodeEntity,
  onPlayClick: () -> Unit,
  onToggleWatched: () -> Unit
) {
  val interaction = remember { MutableInteractionSource() }
  val progress = when {
    episode.isWatched -> 1f
    episode.durationMs > 0L -> (episode.lastPositionMs.toFloat() / episode.durationMs.toFloat()).coerceIn(0f, 1f)
    else -> 0f
  }

  Surface(
    color = ObsidianSurface,
    shape = RoundedCornerShape(14.dp),
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 5.dp)
      .scaleOnPress(interaction)
      .border(1.dp, ObsidianBorder, RoundedCornerShape(14.dp))
      .clip(RoundedCornerShape(14.dp))
      .clickable(interactionSource = interaction, indication = null, onClick = onPlayClick)
      .testTag("episode_row_${episode.id}")
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(10.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      Box(
        modifier = Modifier
          .width(114.dp)
          .height(66.dp)
          .clip(RoundedCornerShape(10.dp))
      ) {
        VideoThumbnail(
          uri = episode.fileUri,
          contentDescription = stringResource(R.string.cd_episode_play, episode.number),
          modifier = Modifier.fillMaxSize(),
          targetWidthPx = 320,
          targetHeightPx = 180
        )
        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.25f))
        )
        Box(
          modifier = Modifier
            .size(30.dp)
            .align(Alignment.Center)
            .clip(CircleShape)
            .background(ObsidianBackground.copy(alpha = 0.8f)),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Rounded.PlayArrow,
            contentDescription = stringResource(R.string.cd_episode_play, episode.number),
            tint = VlcOrange,
            modifier = Modifier.size(18.dp)
          )
        }
        if (progress > 0f) {
          LinearProgressIndicator(
            progress = { progress },
            color = VlcOrange,
            trackColor = ObsidianSurfaceElevated,
            modifier = Modifier
              .align(Alignment.BottomCenter)
              .fillMaxWidth()
              .height(3.dp)
          )
        }
      }

      Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(3.dp)
      ) {
        Text(
          text = stringResource(R.string.detail_episode_label, episode.number),
          style = MaterialTheme.typography.titleMedium,
          color = if (episode.isWatched) SecondaryGray else MaterialTheme.colorScheme.onSurface
        )
        Text(
          text = episode.fileName,
          style = MaterialTheme.typography.bodySmall,
          color = SecondaryGray,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          if (episode.durationMs > 0L) {
            Text(
              text = TimeFormatters.formatDuration(episode.durationMs),
              style = MaterialTheme.typography.labelSmall,
              color = SecondaryGray
            )
          }
          if (episode.isWatched) {
            Surface(
              color = VlcOrangeSoft,
              shape = RoundedCornerShape(6.dp)
            ) {
              Text(
                text = stringResource(R.string.detail_watched_label),
                style = MaterialTheme.typography.labelSmall,
                color = VlcOrange,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
              )
            }
          } else if (episode.lastPositionMs > 0L) {
            Text(
              text = stringResource(
                R.string.detail_resume_at,
                TimeFormatters.formatDuration(episode.lastPositionMs)
              ),
              style = MaterialTheme.typography.labelSmall,
              color = VlcOrange
            )
          }
        }
      }

      IconButton(
        onClick = onToggleWatched,
        modifier = Modifier
          .minimumInteractiveComponentSize()
          .testTag("toggle_watched_${episode.id}")
      ) {
        Icon(
          imageVector = if (episode.isWatched) {
            Icons.Rounded.CheckCircle
          } else {
            Icons.Outlined.CheckCircle
          },
          contentDescription = stringResource(
            if (episode.isWatched) R.string.detail_unmark_watched else R.string.detail_mark_watched
          ),
          tint = if (episode.isWatched) VlcOrange else SecondaryGray
        )
      }
    }
  }
}

@Composable
private fun EditMetadataDialog(
  initialGenre: String,
  initialYear: String,
  onDismiss: () -> Unit,
  onSave: (String, String) -> Unit
) {
  var genre by remember { mutableStateOf(initialGenre) }
  var year by remember { mutableStateOf(initialYear) }

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = ObsidianSurface,
    title = {
      Text(
        text = stringResource(R.string.detail_edit_info_title),
        style = MaterialTheme.typography.titleLarge
      )
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
          value = genre,
          onValueChange = { genre = it },
          label = { Text(stringResource(R.string.detail_genre_label)) },
          placeholder = { Text(stringResource(R.string.detail_genre_placeholder)) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
          value = year,
          onValueChange = { input -> if (input.length <= 4 && input.all { it.isDigit() }) year = input },
          label = { Text(stringResource(R.string.detail_year_label)) },
          placeholder = { Text(stringResource(R.string.detail_year_placeholder)) },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )
      }
    },
    confirmButton = {
      Button(
        onClick = { onSave(genre, year) },
        colors = ButtonDefaults.buttonColors(
          containerColor = VlcOrange,
          contentColor = OnPrimaryBlack
        )
      ) {
        Text(stringResource(R.string.action_save))
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text(stringResource(R.string.action_cancel), color = SecondaryGray)
      }
    }
  )
}
