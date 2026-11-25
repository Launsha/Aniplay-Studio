package com.example.ui.detail

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.LinkOff
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.local.entity.AnimeEntity
import com.example.data.local.entity.EpisodeEntity
import com.example.data.local.entity.PlaylistSummary
import com.example.data.remote.MalAnimeCandidate
import com.example.ui.components.VideoThumbnail
import com.example.ui.components.rememberClampedDominantColor
import com.example.ui.theme.AnimePlayRoleShapes
import com.example.ui.theme.ObsidianBackground
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianDivider
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceElevated
import com.example.ui.theme.OnPrimaryBlack
import com.example.ui.theme.SecondaryGray
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.VlcOrange
import com.example.util.TimeFormatters
import com.example.util.scaleOnPress
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DetailScreen(
  onBack: () -> Unit,
  onEpisodeClick: (Long, Long) -> Unit,
  viewModel: DetailViewModel = hiltViewModel()
) {
  val context = LocalContext.current
  val state by viewModel.uiState.collectAsStateWithLifecycle()
  var showEditDialog by remember { mutableStateOf(false) }
  var showMalSearchDialog by remember { mutableStateOf(false) }
  var showPlaylistDialog by remember { mutableStateOf(false) }
  var synopsisExpanded by remember { mutableStateOf(false) }

  LaunchedEffect(state.introOutroStatusMessage) {
    val msg = state.introOutroStatusMessage ?: return@LaunchedEffect
    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
    viewModel.consumeIntroOutroMessage()
  }

  val anime = state.anime
  val headerTint = rememberClampedDominantColor(
    imageUri = anime?.effectiveCoverUri,
    fallbackColor = ObsidianSurface
  )

  Scaffold(
    containerColor = ObsidianBackground,
    contentWindowInsets = WindowInsets(0)
  ) { innerPadding ->
    LazyColumn(
      contentPadding = PaddingValues(bottom = 32.dp),
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .windowInsetsPadding(WindowInsets.navigationBars)
        .testTag("detail_episode_list")
    ) {
      // Palette-tinted header (no gradient decoration)
      item {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .background(headerTint)
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 20.dp),
          verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            IconButton(
              onClick = onBack,
              modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.35f))
                .testTag("detail_back_button")
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = stringResource(R.string.action_back),
                tint = TextPrimary
              )
            }

            if (anime != null) {
              IconButton(
                onClick = { viewModel.toggleFavorite() },
                modifier = Modifier
                  .size(48.dp)
                  .clip(CircleShape)
                  .background(Color.Black.copy(alpha = 0.35f))
                  .testTag("detail_favorite_button")
              ) {
                Icon(
                  imageVector = if (anime.isFavorite) Icons.Rounded.Star else Icons.Rounded.StarOutline,
                  contentDescription = stringResource(
                    if (anime.isFavorite) R.string.detail_favorite_remove else R.string.detail_favorite_add
                  ),
                  tint = if (anime.isFavorite) VlcOrange else TextPrimary
                )
              }
            }
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.Bottom
          ) {
            VideoThumbnail(
              uri = anime?.effectiveCoverUri,
              contentDescription = stringResource(R.string.cd_anime_thumbnail, anime?.title.orEmpty()),
              modifier = Modifier
                .width(104.dp)
                .aspectRatio(2f / 3f)
                .clip(AnimePlayRoleShapes.Poster),
              targetWidthPx = 300,
              targetHeightPx = 450
            )

            Column(
              modifier = Modifier.weight(1f),
              verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Text(
                text = anime?.title.orEmpty(),
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
              )

              val watchedCount = state.episodes.count { it.isWatched }
              Text(
                text = "${stringResource(R.string.home_episode_count, state.episodes.size)} • ${
                  stringResource(R.string.home_watched_progress, watchedCount, state.episodes.size)
                }",
                style = MaterialTheme.typography.bodySmall,
                color = TextPrimary.copy(alpha = 0.86f)
              )

              anime?.let { item ->
                val dateText = item.releaseDate.ifBlank { item.releaseYear?.toString().orEmpty() }
                val genreAndDate = buildString {
                  append(item.genre)
                  if (dateText.isNotBlank()) append(" • $dateText")
                }
                Text(
                  text = genreAndDate,
                  style = MaterialTheme.typography.bodySmall,
                  color = SecondaryGray,
                  maxLines = 2,
                  overflow = TextOverflow.Ellipsis
                )

                FlowRow(
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                  verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                  item.malScore?.let { score ->
                    Text(
                      text = stringResource(R.string.mal_score_badge, score),
                      style = MaterialTheme.typography.labelSmall,
                      color = TextPrimary,
                      modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.35f), AnimePlayRoleShapes.Chip)
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                  }
                  Row(
                    modifier = Modifier
                      .clip(AnimePlayRoleShapes.Chip)
                      .background(Color.Black.copy(alpha = 0.35f))
                      .clickable { showEditDialog = true }
                      .padding(horizontal = 10.dp, vertical = 5.dp)
                      .testTag("detail_edit_button"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                  ) {
                    Icon(
                      imageVector = Icons.Rounded.Edit,
                      contentDescription = stringResource(R.string.detail_edit_info),
                      tint = TextPrimary,
                      modifier = Modifier.size(13.dp)
                    )
                    Text(
                      text = stringResource(R.string.detail_edit_info),
                      style = MaterialTheme.typography.labelSmall,
                      color = TextPrimary
                    )
                  }
                }
              }
            }
          }
        }
      }

      // Primary Play CTA + Secondary Action Buttons + Sections separated by spacing & dividers
      item {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 20.dp),
          verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
          val nextEp = state.nextToPlayEpisode
          Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (nextEp != null) {
              val playInteraction = remember { MutableInteractionSource() }
              Button(
                onClick = { onEpisodeClick(nextEp.id, -1L) },
                interactionSource = playInteraction,
                colors = ButtonDefaults.buttonColors(
                  containerColor = VlcOrange,
                  contentColor = OnPrimaryBlack
                ),
                shape = AnimePlayRoleShapes.Button,
                modifier = Modifier
                  .fillMaxWidth()
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

            if (anime != null) {
              val statusLabel = when (anime.malStatus) {
                "completed" -> stringResource(R.string.mal_status_completed)
                "on_hold" -> stringResource(R.string.mal_status_on_hold)
                "dropped" -> stringResource(R.string.mal_status_dropped)
                "plan_to_watch" -> stringResource(R.string.mal_status_plan)
                else -> stringResource(R.string.mal_status_watching)
              }
              val shareText = stringResource(
                R.string.detail_share_text,
                anime.title,
                state.episodes.size,
                statusLabel
              )
              val shareChooserTitle = stringResource(R.string.detail_share_anime)

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                OutlinedButton(
                  onClick = { viewModel.scanAllEpisodesIntroOutro() },
                  enabled = !state.isScanningIntroOutro,
                  shape = AnimePlayRoleShapes.Button,
                  border = BorderStroke(1.dp, ObsidianBorder),
                  contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                  modifier = Modifier
                    .weight(1.35f)
                    .height(48.dp)
                    .testTag("detail_scan_intro_outro_button")
                ) {
                  if (state.isScanningIntroOutro) {
                    CircularProgressIndicator(
                      color = VlcOrange,
                      strokeWidth = 2.dp,
                      modifier = Modifier.size(16.dp)
                    )
                  } else {
                    Icon(
                      imageVector = Icons.Rounded.AutoFixHigh,
                      contentDescription = null,
                      tint = TextPrimary,
                      modifier = Modifier.size(16.dp)
                    )
                  }
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text = stringResource(R.string.detail_scan_intro_outro),
                    style = MaterialTheme.typography.labelMedium,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                  )
                }

                OutlinedButton(
                  onClick = { showPlaylistDialog = true },
                  shape = AnimePlayRoleShapes.Button,
                  border = BorderStroke(1.dp, ObsidianBorder),
                  contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                  modifier = Modifier
                    .weight(0.95f)
                    .height(48.dp)
                    .testTag("detail_playlist_button")
                ) {
                  Icon(
                    imageVector = Icons.AutoMirrored.Rounded.PlaylistAdd,
                    contentDescription = null,
                    tint = TextPrimary,
                    modifier = Modifier.size(17.dp)
                  )
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(
                    text = stringResource(R.string.detail_add_to_playlist),
                    style = MaterialTheme.typography.labelMedium,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                  )
                }

                OutlinedButton(
                  onClick = {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                      type = "text/plain"
                      putExtra(Intent.EXTRA_TEXT, shareText)
                    }
                    context.startActivity(Intent.createChooser(intent, shareChooserTitle))
                  },
                  shape = AnimePlayRoleShapes.Button,
                  border = BorderStroke(1.dp, ObsidianBorder),
                  contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                  modifier = Modifier
                    .weight(0.85f)
                    .height(48.dp)
                    .testTag("detail_share_button")
                ) {
                  Icon(
                    imageVector = Icons.Rounded.Share,
                    contentDescription = null,
                    tint = TextPrimary,
                    modifier = Modifier.size(16.dp)
                  )
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(
                    text = stringResource(R.string.detail_share_anime),
                    style = MaterialTheme.typography.labelMedium,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                  )
                }
              }
            }
          }

          if (anime != null) {
            HorizontalDivider(color = ObsidianDivider)

            // Synopsis section (open layout, no box)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
              Text(
                text = stringResource(R.string.detail_synopsis_label),
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary
              )
              if (anime.synopsis.isBlank()) {
                Text(
                  text = stringResource(R.string.detail_synopsis_empty),
                  style = MaterialTheme.typography.bodyMedium,
                  color = SecondaryGray
                )
              } else {
                Text(
                  text = anime.synopsis,
                  style = MaterialTheme.typography.bodyMedium,
                  color = SecondaryGray,
                  maxLines = if (synopsisExpanded) Int.MAX_VALUE else 3,
                  overflow = TextOverflow.Ellipsis
                )
                if (anime.synopsis.length > 140) {
                  Text(
                    text = stringResource(
                      if (synopsisExpanded) R.string.detail_read_less else R.string.detail_read_more
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = TextPrimary,
                    modifier = Modifier
                      .clickable { synopsisExpanded = !synopsisExpanded }
                      .padding(vertical = 4.dp)
                  )
                }
              }
            }

            HorizontalDivider(color = ObsidianDivider)

            // MyAnimeList Tracking section
            MalTrackingSection(
              anime = anime,
              watchedEpisodes = state.episodes.count { it.isWatched },
              totalEpisodes = state.episodes.size,
              onSearchMalClick = {
                showMalSearchDialog = true
                viewModel.searchMyAnimeList(anime.title)
              },
              onOpenMalWeb = { malId ->
                val intent = Intent(
                  Intent.ACTION_VIEW,
                  Uri.parse("https://myanimelist.net/anime/$malId")
                )
                context.startActivity(intent)
              },
              onUnlinkMal = { viewModel.unlinkMal() },
              onSelectStatus = viewModel::updateMalStatus,
              onSelectUserScore = viewModel::updateMalUserScore
            )

            if (state.bookmarks.isNotEmpty()) {
              HorizontalDivider(color = ObsidianDivider)
              Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                  text = stringResource(R.string.detail_bookmarks_title),
                  style = MaterialTheme.typography.titleMedium,
                  color = TextPrimary
                )
                state.bookmarks.forEach { bm ->
                  Row(
                    modifier = Modifier
                      .fillMaxWidth()
                      .clickable { onEpisodeClick(bm.episodeId, bm.positionMs) }
                      .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                  ) {
                    Text(
                      text = "${stringResource(R.string.detail_episode_label, bm.episodeNumber)} • ${
                        TimeFormatters.formatDuration(bm.positionMs)
                      }",
                      style = MaterialTheme.typography.labelMedium,
                      color = TextPrimary
                    )
                    Text(
                      text = bm.label,
                      style = MaterialTheme.typography.bodySmall,
                      color = SecondaryGray,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                      modifier = Modifier.weight(1f)
                    )
                    IconButton(
                      onClick = { viewModel.deleteBookmark(bm.id) },
                      modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                      Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = stringResource(R.string.action_delete),
                        tint = SecondaryGray,
                        modifier = Modifier.size(18.dp)
                      )
                    }
                  }
                }
              }
            }
          }

          HorizontalDivider(color = ObsidianDivider)

          Text(
            text = stringResource(R.string.detail_episode_list_title),
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary
          )
        }
      }

      if (state.episodes.isEmpty()) {
        item {
          Text(
            text = stringResource(R.string.detail_no_episodes),
            style = MaterialTheme.typography.bodyMedium,
            color = SecondaryGray,
            modifier = Modifier.padding(horizontal = 16.dp)
          )
        }
      } else {
        items(state.episodes, key = { it.id }) { episode ->
          EpisodeRowItem(
            episode = episode,
            onPlayClick = { onEpisodeClick(episode.id, -1L) },
            onToggleWatched = { viewModel.toggleEpisodeWatched(episode) }
          )
        }
      }
    }
  }

  if (showEditDialog && anime != null) {
    EditAnimeDetailsDialog(
      anime = anime,
      onCopyPickedCover = { uri -> viewModel.copyPickedCoverToInternalStorage(uri) },
      onDismiss = { showEditDialog = false },
      onSave = { title, coverUri, synopsis, genre, releaseDate ->
        viewModel.updateAnimeDetails(
          title = title,
          customCoverUri = coverUri,
          synopsis = synopsis,
          genre = genre,
          releaseDate = releaseDate
        )
        showEditDialog = false
      }
    )
  }

  if (showMalSearchDialog && anime != null) {
    MalSearchDialog(
      initialQuery = anime.title,
      searchState = state.malSearch,
      onSearch = viewModel::searchMyAnimeList,
      onSelectCandidate = { candidate ->
        viewModel.linkMalCandidate(candidate)
        showMalSearchDialog = false
      },
      onDismiss = { showMalSearchDialog = false }
    )
  }

  if (showPlaylistDialog && anime != null) {
    ManageAnimePlaylistDialog(
      playlists = state.playlists,
      memberPlaylistIds = state.animePlaylistIds,
      onToggleMembership = viewModel::togglePlaylistMembership,
      onCreatePlaylist = viewModel::createPlaylistAndAddAnime,
      onDismiss = { showPlaylistDialog = false }
    )
  }
}

@Composable
private fun MalTrackingSection(
  anime: AnimeEntity,
  watchedEpisodes: Int,
  totalEpisodes: Int,
  onSearchMalClick: () -> Unit,
  onOpenMalWeb: (Int) -> Unit,
  onUnlinkMal: () -> Unit,
  onSelectStatus: (String) -> Unit,
  onSelectUserScore: (Int?) -> Unit
) {
  val statuses = listOf(
    "watching" to stringResource(R.string.mal_status_watching),
    "completed" to stringResource(R.string.mal_status_completed),
    "on_hold" to stringResource(R.string.mal_status_on_hold),
    "dropped" to stringResource(R.string.mal_status_dropped),
    "plan_to_watch" to stringResource(R.string.mal_status_plan)
  )

  val neutralChipColors = FilterChipDefaults.filterChipColors(
    selectedContainerColor = TextPrimary,
    selectedLabelColor = OnPrimaryBlack,
    selectedLeadingIconColor = OnPrimaryBlack,
    containerColor = ObsidianSurfaceElevated,
    labelColor = SecondaryGray,
    iconColor = SecondaryGray
  )
  val neutralChipBorder = FilterChipDefaults.filterChipBorder(
    enabled = true,
    selected = false,
    borderColor = Color.Transparent,
    selectedBorderColor = Color.Transparent
  )

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("mal_tracking_section"),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
          text = stringResource(R.string.mal_section_title),
          style = MaterialTheme.typography.titleMedium,
          color = TextPrimary
        )
        Text(
          text = stringResource(R.string.mal_progress_label, watchedEpisodes, totalEpisodes),
          style = MaterialTheme.typography.labelSmall,
          color = SecondaryGray
        )
      }
      Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        if (anime.malId != null) {
          OutlinedButton(
            onClick = { onOpenMalWeb(anime.malId) },
            shape = AnimePlayRoleShapes.Button,
            border = BorderStroke(1.dp, ObsidianBorder),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            modifier = Modifier.minimumInteractiveComponentSize()
          ) {
            Text(
              text = stringResource(R.string.mal_open_web),
              style = MaterialTheme.typography.labelSmall,
              color = TextPrimary
            )
          }
          IconButton(
            onClick = onUnlinkMal,
            modifier = Modifier.minimumInteractiveComponentSize()
          ) {
            Icon(
              imageVector = Icons.Rounded.LinkOff,
              contentDescription = stringResource(R.string.mal_unlink_button),
              tint = SecondaryGray,
              modifier = Modifier.size(18.dp)
            )
          }
        } else {
          OutlinedButton(
            onClick = onSearchMalClick,
            shape = AnimePlayRoleShapes.Button,
            border = BorderStroke(1.dp, ObsidianBorder),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
            modifier = Modifier
              .minimumInteractiveComponentSize()
              .testTag("mal_search_button")
          ) {
            Icon(
              imageVector = Icons.Rounded.Search,
              contentDescription = null,
              tint = TextPrimary,
              modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = stringResource(R.string.mal_search_button),
              style = MaterialTheme.typography.labelSmall,
              color = TextPrimary
            )
          }
        }
      }
    }

    Text(
      text = stringResource(R.string.mal_status_label),
      style = MaterialTheme.typography.labelSmall,
      color = SecondaryGray
    )
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      items(statuses) { (code, label) ->
        FilterChip(
          selected = anime.malStatus == code,
          onClick = { onSelectStatus(code) },
          shape = AnimePlayRoleShapes.Chip,
          border = neutralChipBorder,
          label = { Text(text = label, style = MaterialTheme.typography.labelSmall) },
          colors = neutralChipColors
        )
      }
    }

    Text(
      text = stringResource(R.string.mal_user_score_label),
      style = MaterialTheme.typography.labelSmall,
      color = SecondaryGray
    )
    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
      items((1..10).toList()) { score ->
        FilterChip(
          selected = anime.malUserScore == score,
          onClick = { onSelectUserScore(if (anime.malUserScore == score) null else score) },
          shape = AnimePlayRoleShapes.Chip,
          border = neutralChipBorder,
          leadingIcon = {
            Icon(
              imageVector = Icons.Rounded.Star,
              contentDescription = null,
              modifier = Modifier.size(13.dp)
            )
          },
          label = { Text(text = score.toString(), style = MaterialTheme.typography.labelSmall) },
          colors = neutralChipColors
        )
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
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

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .scaleOnPress(interaction)
        .clickable(interactionSource = interaction, indication = null, onClick = onPlayClick)
        .padding(vertical = 10.dp)
        .testTag("episode_row_${episode.id}"),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      Box(
        modifier = Modifier
          .width(114.dp)
          .height(66.dp)
          .clip(AnimePlayRoleShapes.Poster)
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
          color = if (episode.isWatched) SecondaryGray else TextPrimary
        )
        Text(
          text = episode.fileName,
          style = MaterialTheme.typography.bodySmall,
          color = SecondaryGray,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          if (episode.durationMs > 0L) {
            Text(
              text = TimeFormatters.formatDuration(episode.durationMs),
              style = MaterialTheme.typography.labelSmall,
              color = SecondaryGray
            )
          }
          if (episode.isWatched) {
            Text(
              text = stringResource(R.string.detail_watched_label),
              style = MaterialTheme.typography.labelSmall,
              color = SecondaryGray
            )
          } else if (episode.lastPositionMs > 0L) {
            Text(
              text = stringResource(
                R.string.detail_resume_at,
                TimeFormatters.formatDuration(episode.lastPositionMs)
              ),
              style = MaterialTheme.typography.labelSmall,
              color = TextPrimary
            )
          }
          if (episode.introStartMs >= 0L && episode.introEndMs > episode.introStartMs) {
            Text(
              text = stringResource(
                R.string.detail_intro_badge,
                "${TimeFormatters.formatDuration(episode.introStartMs)}-${TimeFormatters.formatDuration(episode.introEndMs)}"
              ),
              style = MaterialTheme.typography.labelSmall,
              color = SecondaryGray
            )
          }
          if (episode.outroStartMs >= 0L && episode.outroEndMs > episode.outroStartMs) {
            Text(
              text = stringResource(
                R.string.detail_outro_badge,
                "${TimeFormatters.formatDuration(episode.outroStartMs)}-${TimeFormatters.formatDuration(episode.outroEndMs)}"
              ),
              style = MaterialTheme.typography.labelSmall,
              color = SecondaryGray
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
    HorizontalDivider(color = ObsidianDivider)
  }
}

@Composable
private fun EditAnimeDetailsDialog(
  anime: AnimeEntity,
  onCopyPickedCover: suspend (Uri) -> String?,
  onDismiss: () -> Unit,
  onSave: (title: String, coverUri: String?, synopsis: String, genre: String, releaseDate: String) -> Unit
) {
  val scope = rememberCoroutineScope()
  var title by remember(anime.id) { mutableStateOf(anime.title) }
  var coverUri by remember(anime.id) { mutableStateOf(anime.customCoverUri) }
  var synopsis by remember(anime.id) { mutableStateOf(anime.synopsis) }
  var genre by remember(anime.id) { mutableStateOf(anime.genre) }
  var releaseDate by remember(anime.id) {
    mutableStateOf(anime.releaseDate.ifBlank { anime.releaseYear?.toString().orEmpty() })
  }

  val photoPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia()
  ) { pickedUri ->
    if (pickedUri != null) {
      scope.launch {
        coverUri = onCopyPickedCover(pickedUri) ?: pickedUri.toString()
      }
    }
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = ObsidianSurface,
    shape = AnimePlayRoleShapes.Dialog,
    title = {
      Text(
        text = stringResource(R.string.detail_edit_info_title),
        style = MaterialTheme.typography.titleLarge,
        color = TextPrimary
      )
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .heightIn(max = 460.dp)
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          VideoThumbnail(
            uri = coverUri ?: anime.coverUri,
            contentDescription = stringResource(R.string.cd_anime_thumbnail, title),
            modifier = Modifier
              .size(width = 72.dp, height = 104.dp)
              .clip(AnimePlayRoleShapes.Poster),
            targetWidthPx = 220,
            targetHeightPx = 300
          )
          Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedButton(
              onClick = {
                photoPickerLauncher.launch(
                  PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
              },
              shape = AnimePlayRoleShapes.Button,
              border = BorderStroke(1.dp, ObsidianBorder),
              modifier = Modifier.minimumInteractiveComponentSize()
            ) {
              Icon(
                imageVector = Icons.Rounded.Image,
                contentDescription = null,
                tint = TextPrimary,
                modifier = Modifier.size(16.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = stringResource(R.string.detail_pick_cover),
                color = TextPrimary,
                style = MaterialTheme.typography.labelMedium
              )
            }
            if (!coverUri.isNullOrBlank()) {
              TextButton(
                onClick = { coverUri = null },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
              ) {
                Text(
                  text = stringResource(R.string.detail_reset_cover),
                  color = SecondaryGray,
                  style = MaterialTheme.typography.labelSmall
                )
              }
            }
          }
        }

        OutlinedTextField(
          value = title,
          onValueChange = { title = it },
          label = { Text(stringResource(R.string.detail_title_label)) },
          singleLine = true,
          shape = AnimePlayRoleShapes.Input,
          modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
          value = synopsis,
          onValueChange = { synopsis = it },
          label = { Text(stringResource(R.string.detail_synopsis_label)) },
          placeholder = { Text(stringResource(R.string.detail_synopsis_placeholder)) },
          minLines = 3,
          maxLines = 5,
          shape = AnimePlayRoleShapes.Input,
          modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
          value = genre,
          onValueChange = { genre = it },
          label = { Text(stringResource(R.string.detail_genre_label)) },
          placeholder = { Text(stringResource(R.string.detail_genre_placeholder)) },
          singleLine = true,
          shape = AnimePlayRoleShapes.Input,
          modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
          value = releaseDate,
          onValueChange = { releaseDate = it },
          label = { Text(stringResource(R.string.detail_release_date_label)) },
          placeholder = { Text(stringResource(R.string.detail_release_date_placeholder)) },
          singleLine = true,
          shape = AnimePlayRoleShapes.Input,
          modifier = Modifier.fillMaxWidth()
        )
      }
    },
    confirmButton = {
      Button(
        onClick = { onSave(title, coverUri, synopsis, genre, releaseDate) },
        shape = AnimePlayRoleShapes.Button,
        colors = ButtonDefaults.buttonColors(
          containerColor = TextPrimary,
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

@Composable
private fun MalSearchDialog(
  initialQuery: String,
  searchState: MalSearchUiState,
  onSearch: (String) -> Unit,
  onSelectCandidate: (MalAnimeCandidate) -> Unit,
  onDismiss: () -> Unit
) {
  var query by remember { mutableStateOf(initialQuery) }

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = ObsidianSurface,
    shape = AnimePlayRoleShapes.Dialog,
    title = {
      Text(
        text = stringResource(R.string.mal_search_dialog_title),
        style = MaterialTheme.typography.titleLarge,
        color = TextPrimary
      )
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .heightIn(max = 420.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text(stringResource(R.string.mal_search_hint)) },
            singleLine = true,
            shape = AnimePlayRoleShapes.Input,
            modifier = Modifier.weight(1f)
          )
          IconButton(
            onClick = { onSearch(query) },
            modifier = Modifier
              .size(48.dp)
              .clip(AnimePlayRoleShapes.Button)
              .background(TextPrimary)
          ) {
            Icon(
              imageVector = Icons.Rounded.Search,
              contentDescription = stringResource(R.string.action_search),
              tint = OnPrimaryBlack
            )
          }
        }

        when {
          searchState.isSearching -> {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp),
              horizontalArrangement = Arrangement.Center,
              verticalAlignment = Alignment.CenterVertically
            ) {
              CircularProgressIndicator(color = VlcOrange, modifier = Modifier.size(22.dp))
              Spacer(modifier = Modifier.width(10.dp))
              Text(
                text = stringResource(R.string.mal_searching),
                style = MaterialTheme.typography.bodySmall,
                color = SecondaryGray
              )
            }
          }

          searchState.hasError -> {
            Text(
              text = stringResource(R.string.mal_search_error),
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.error
            )
          }

          searchState.hasSearched && searchState.results.isEmpty() -> {
            Text(
              text = stringResource(R.string.mal_search_empty),
              style = MaterialTheme.typography.bodySmall,
              color = SecondaryGray
            )
          }

          else -> {
            LazyColumn(
              verticalArrangement = Arrangement.spacedBy(10.dp),
              modifier = Modifier.weight(1f, fill = false)
            ) {
              items(searchState.results, key = { it.malId }) { item ->
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectCandidate(item) }
                    .padding(vertical = 6.dp),
                  horizontalArrangement = Arrangement.spacedBy(10.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  VideoThumbnail(
                    uri = item.coverUrl,
                    contentDescription = item.title,
                    modifier = Modifier
                      .size(width = 48.dp, height = 72.dp)
                      .clip(AnimePlayRoleShapes.Poster),
                    targetWidthPx = 140,
                    targetHeightPx = 200
                  )
                  Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                  ) {
                    Text(
                      text = item.title,
                      style = MaterialTheme.typography.titleSmall,
                      color = TextPrimary,
                      maxLines = 2,
                      overflow = TextOverflow.Ellipsis
                    )
                    Text(
                      text = buildString {
                        append(item.genre)
                        if (item.releaseDate.isNotBlank()) append(" • ${item.releaseDate}")
                      },
                      style = MaterialTheme.typography.labelSmall,
                      color = SecondaryGray,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis
                    )
                    item.score?.let { sc ->
                      Text(
                        text = stringResource(R.string.mal_score_badge, sc),
                        style = MaterialTheme.typography.labelSmall,
                        color = SecondaryGray
                      )
                    }
                  }
                }
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

@Composable
private fun ManageAnimePlaylistDialog(
  playlists: List<PlaylistSummary>,
  memberPlaylistIds: Set<Long>,
  onToggleMembership: (Long, Boolean) -> Unit,
  onCreatePlaylist: (String) -> Unit,
  onDismiss: () -> Unit
) {
  var newPlaylistName by remember { mutableStateOf("") }

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = ObsidianSurface,
    shape = AnimePlayRoleShapes.Dialog,
    title = {
      Text(
        text = stringResource(R.string.detail_playlist_dialog_title),
        style = MaterialTheme.typography.titleLarge,
        color = TextPrimary
      )
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .heightIn(max = 380.dp)
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        if (playlists.isEmpty()) {
          Text(
            text = stringResource(R.string.playlist_empty),
            style = MaterialTheme.typography.bodySmall,
            color = SecondaryGray
          )
        } else {
          playlists.forEach { playlist ->
            val checked = playlist.id in memberPlaylistIds
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggleMembership(playlist.id, !checked) }
                .padding(vertical = 4.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Checkbox(
                checked = checked,
                onCheckedChange = { onToggleMembership(playlist.id, it) },
                colors = CheckboxDefaults.colors(
                  checkedColor = VlcOrange,
                  checkmarkColor = OnPrimaryBlack
                )
              )
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = playlist.name,
                  style = MaterialTheme.typography.bodyMedium,
                  color = TextPrimary,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
                Text(
                  text = stringResource(R.string.playlist_item_count, playlist.animeCount),
                  style = MaterialTheme.typography.labelSmall,
                  color = SecondaryGray
                )
              }
            }
          }
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          OutlinedTextField(
            value = newPlaylistName,
            onValueChange = { newPlaylistName = it },
            placeholder = { Text(stringResource(R.string.playlist_name_hint)) },
            singleLine = true,
            shape = AnimePlayRoleShapes.Input,
            modifier = Modifier.weight(1f)
          )
          Button(
            onClick = {
              val trimmed = newPlaylistName.trim()
              if (trimmed.isNotBlank()) {
                onCreatePlaylist(trimmed)
                newPlaylistName = ""
              }
            },
            enabled = newPlaylistName.isNotBlank(),
            shape = AnimePlayRoleShapes.Button,
            colors = ButtonDefaults.buttonColors(
              containerColor = TextPrimary,
              contentColor = OnPrimaryBlack
            ),
            modifier = Modifier.height(48.dp)
          ) {
            Text(stringResource(R.string.action_add), style = MaterialTheme.typography.labelMedium)
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

