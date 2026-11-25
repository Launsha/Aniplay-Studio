package com.example.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.automirrored.rounded.ViewList
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.entity.AnimeWithStats
import com.example.ui.components.VideoThumbnail
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
import com.example.util.scaleOnPress
import java.util.Locale

@Composable
fun PustakaTab(
  state: HomeUiState,
  onAnimeClick: (Long) -> Unit,
  onToggleFavorite: (Long, Boolean) -> Unit,
  onSearchQueryChange: (String) -> Unit,
  onSelectGenre: (String?) -> Unit,
  onSelectYear: (Int?) -> Unit,
  onSelectPlaylist: (Long?) -> Unit,
  onCreatePlaylist: (String) -> Unit,
  onDeletePlaylist: (Long) -> Unit,
  onToggleFavoritesOnly: () -> Unit,
  onSetSortOption: (LibrarySortOption) -> Unit,
  onToggleViewMode: () -> Unit,
  onClearAllFilters: () -> Unit,
  onRefresh: () -> Unit,
  onOpenSettings: () -> Unit
) {
  var showSortMenu by remember { mutableStateOf(false) }
  var showCreatePlaylistDialog by remember { mutableStateOf(false) }

  val isFiltering = state.searchQuery.isNotBlank() ||
    state.selectedGenre != null ||
    state.selectedYear != null ||
    state.selectedMalStatus != null ||
    state.selectedPlaylistId != null ||
    state.favoritesOnly

  val columns = if (state.isListViewMode) {
    GridCells.Fixed(1)
  } else {
    GridCells.Adaptive(minSize = 152.dp)
  }

  val neutralChipColors = FilterChipDefaults.filterChipColors(
    selectedContainerColor = TextPrimary,
    selectedLabelColor = OnPrimaryBlack,
    selectedLeadingIconColor = OnPrimaryBlack,
    selectedTrailingIconColor = OnPrimaryBlack,
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
      .fillMaxSize()
      .testTag("pustaka_tab")
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .background(ObsidianBackground)
        .padding(horizontal = 16.dp, vertical = 10.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        OutlinedTextField(
          value = state.searchQuery,
          onValueChange = onSearchQueryChange,
          placeholder = {
            Text(
              text = stringResource(R.string.home_search_hint),
              style = MaterialTheme.typography.bodyMedium,
              color = SecondaryGray
            )
          },
          leadingIcon = {
            Icon(
              imageVector = Icons.Rounded.Search,
              contentDescription = null,
              tint = SecondaryGray
            )
          },
          trailingIcon = {
            if (state.searchQuery.isNotEmpty()) {
              IconButton(onClick = { onSearchQueryChange("") }) {
                Icon(
                  imageVector = Icons.Rounded.Close,
                  contentDescription = stringResource(R.string.action_clear_search),
                  tint = SecondaryGray
                )
              }
            }
          },
          singleLine = true,
          shape = AnimePlayRoleShapes.Input,
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = TextPrimary,
            unfocusedBorderColor = ObsidianBorder,
            focusedContainerColor = ObsidianSurface,
            unfocusedContainerColor = ObsidianSurface,
            cursorColor = TextPrimary
          ),
          modifier = Modifier
            .weight(1f)
            .testTag("search_input")
        )

        Box {
          IconButton(
            onClick = { showSortMenu = true },
            modifier = Modifier
              .size(48.dp)
              .clip(AnimePlayRoleShapes.Button)
              .background(ObsidianSurface)
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Rounded.Sort,
              contentDescription = stringResource(R.string.library_sort_recent),
              tint = TextPrimary
            )
          }
          DropdownMenu(
            expanded = showSortMenu,
            onDismissRequest = { showSortMenu = false },
            containerColor = ObsidianSurface
          ) {
            val options = listOf(
              LibrarySortOption.RECENT to R.string.library_sort_recent,
              LibrarySortOption.TITLE to R.string.library_sort_title,
              LibrarySortOption.MAL_SCORE to R.string.library_sort_score,
              LibrarySortOption.YEAR to R.string.library_sort_year
            )
            options.forEach { (option, labelRes) ->
              DropdownMenuItem(
                text = {
                  Text(
                    text = stringResource(labelRes),
                    color = if (state.sortOption == option) TextPrimary else SecondaryGray
                  )
                },
                onClick = {
                  onSetSortOption(option)
                  showSortMenu = false
                }
              )
            }
          }
        }

        IconButton(
          onClick = onToggleViewMode,
          modifier = Modifier
            .size(48.dp)
            .clip(AnimePlayRoleShapes.Button)
            .background(ObsidianSurface)
        ) {
          Icon(
            imageVector = if (state.isListViewMode) Icons.Rounded.GridView else Icons.AutoMirrored.Rounded.ViewList,
            contentDescription = stringResource(R.string.library_toggle_view),
            tint = TextPrimary
          )
        }
      }

      LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
          FilterChip(
            selected = state.favoritesOnly,
            onClick = onToggleFavoritesOnly,
            shape = AnimePlayRoleShapes.Chip,
            border = neutralChipBorder,
            leadingIcon = {
              Icon(
                imageVector = Icons.Rounded.Star,
                contentDescription = null,
                modifier = Modifier.size(14.dp)
              )
            },
            label = {
              Text(
                text = stringResource(R.string.library_filter_favorites_only),
                style = MaterialTheme.typography.labelSmall
              )
            },
            colors = neutralChipColors
          )
        }
        item {
          FilterChip(
            selected = state.selectedGenre == null &&
              state.selectedYear == null &&
              state.selectedPlaylistId == null &&
              !state.favoritesOnly,
            onClick = onClearAllFilters,
            shape = AnimePlayRoleShapes.Chip,
            border = neutralChipBorder,
            label = {
              Text(
                text = stringResource(R.string.home_filter_all_genres),
                style = MaterialTheme.typography.labelSmall
              )
            },
            colors = neutralChipColors
          )
        }
        items(state.availableGenres) { genre ->
          FilterChip(
            selected = state.selectedGenre == genre,
            onClick = { onSelectGenre(genre) },
            shape = AnimePlayRoleShapes.Chip,
            border = neutralChipBorder,
            label = { Text(text = genre, style = MaterialTheme.typography.labelSmall) },
            colors = neutralChipColors
          )
        }
        items(state.availableYears) { year ->
          FilterChip(
            selected = state.selectedYear == year,
            onClick = { onSelectYear(year) },
            shape = AnimePlayRoleShapes.Chip,
            border = neutralChipBorder,
            label = { Text(text = year.toString(), style = MaterialTheme.typography.labelSmall) },
            colors = neutralChipColors
          )
        }
      }

      LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        item {
          FilterChip(
            selected = false,
            onClick = { showCreatePlaylistDialog = true },
            shape = AnimePlayRoleShapes.Chip,
            border = neutralChipBorder,
            leadingIcon = {
              Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = null,
                modifier = Modifier.size(14.dp)
              )
            },
            label = {
              Text(
                text = stringResource(R.string.playlist_create_button),
                style = MaterialTheme.typography.labelSmall
              )
            },
            colors = neutralChipColors,
            modifier = Modifier.testTag("create_playlist_chip")
          )
        }
        items(state.playlists, key = { it.id }) { playlist ->
          val isSelected = state.selectedPlaylistId == playlist.id
          FilterChip(
            selected = isSelected,
            onClick = { onSelectPlaylist(playlist.id) },
            shape = AnimePlayRoleShapes.Chip,
            border = neutralChipBorder,
            leadingIcon = {
              Icon(
                imageVector = Icons.AutoMirrored.Rounded.PlaylistPlay,
                contentDescription = null,
                modifier = Modifier.size(15.dp)
              )
            },
            trailingIcon = if (isSelected) {
              {
                Icon(
                  imageVector = Icons.Rounded.DeleteOutline,
                  contentDescription = stringResource(R.string.action_delete),
                  modifier = Modifier
                    .size(16.dp)
                    .clickable { onDeletePlaylist(playlist.id) }
                )
              }
            } else {
              null
            },
            label = {
              Text(
                text = "${playlist.name} (${playlist.animeCount})",
                style = MaterialTheme.typography.labelSmall
              )
            },
            colors = neutralChipColors
          )
        }
      }
    }

    HorizontalDivider(color = ObsidianDivider)

    LazyVerticalGrid(
      columns = columns,
      contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 32.dp),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      verticalArrangement = Arrangement.spacedBy(20.dp),
      modifier = Modifier
        .fillMaxSize()
        .testTag("anime_grid")
    ) {
      if (state.animeList.isEmpty() && state.hasScannedOnce && !state.isScanning) {
        item(span = { GridItemSpan(maxLineSpan) }) {
          EmptyLibraryState(
            isFiltered = isFiltering,
            foldersLabel = state.configuredFoldersLabel,
            onRefresh = onRefresh,
            onOpenSettings = onOpenSettings,
            onClearSearch = onClearAllFilters
          )
        }
      } else {
        items(state.animeList, key = { it.anime.id }) { animeItem ->
          if (state.isListViewMode) {
            AnimeListRowCard(
              item = animeItem,
              onClick = { onAnimeClick(animeItem.anime.id) },
              onToggleFavorite = { onToggleFavorite(animeItem.anime.id, animeItem.anime.isFavorite) }
            )
          } else {
            AnimeGridCard(
              item = animeItem,
              onClick = { onAnimeClick(animeItem.anime.id) },
              onToggleFavorite = { onToggleFavorite(animeItem.anime.id, animeItem.anime.isFavorite) }
            )
          }
        }
      }
    }
  }

  if (showCreatePlaylistDialog) {
    CreatePlaylistDialog(
      onCreate = { name ->
        onCreatePlaylist(name)
        showCreatePlaylistDialog = false
      },
      onDismiss = { showCreatePlaylistDialog = false }
    )
  }
}

@Composable
private fun CreatePlaylistDialog(
  onCreate: (String) -> Unit,
  onDismiss: () -> Unit
) {
  var name by remember { mutableStateOf("") }
  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = ObsidianSurface,
    shape = AnimePlayRoleShapes.Dialog,
    title = {
      Text(
        text = stringResource(R.string.playlist_create_title),
        style = MaterialTheme.typography.titleLarge,
        color = TextPrimary
      )
    },
    text = {
      OutlinedTextField(
        value = name,
        onValueChange = { name = it },
        placeholder = { Text(stringResource(R.string.playlist_name_hint)) },
        singleLine = true,
        shape = AnimePlayRoleShapes.Input,
        modifier = Modifier.fillMaxWidth()
      )
    },
    confirmButton = {
      Button(
        onClick = {
          val clean = name.trim()
          if (clean.isNotBlank()) onCreate(clean)
        },
        enabled = name.isNotBlank(),
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
private fun AnimeGridCard(
  item: AnimeWithStats,
  onClick: () -> Unit,
  onToggleFavorite: () -> Unit
) {
  val interaction = remember { MutableInteractionSource() }
  val watchedFraction = if (item.anime.episodeCount > 0) {
    (item.watchedCount.toFloat() / item.anime.episodeCount.toFloat()).coerceIn(0f, 1f)
  } else {
    0f
  }

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .scaleOnPress(interaction)
      .clickable(interactionSource = interaction, indication = null, onClick = onClick)
      .testTag("anime_card_${item.anime.id}"),
    verticalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .aspectRatio(2f / 3f)
        .clip(AnimePlayRoleShapes.Poster)
    ) {
      VideoThumbnail(
        uri = item.anime.effectiveCoverUri,
        contentDescription = stringResource(R.string.cd_anime_thumbnail, item.anime.title),
        modifier = Modifier.fillMaxSize(),
        targetWidthPx = 360,
        targetHeightPx = 540
      )

      Text(
        text = stringResource(R.string.home_episode_count, item.anime.episodeCount),
        style = MaterialTheme.typography.labelSmall,
        color = TextPrimary,
        modifier = Modifier
          .align(Alignment.TopEnd)
          .padding(8.dp)
          .background(Color.Black.copy(alpha = 0.68f), AnimePlayRoleShapes.Poster)
          .padding(horizontal = 8.dp, vertical = 4.dp)
      )

      IconButton(
        onClick = onToggleFavorite,
        modifier = Modifier
          .align(Alignment.BottomEnd)
          .minimumInteractiveComponentSize()
      ) {
        Icon(
          imageVector = if (item.anime.isFavorite) Icons.Rounded.Star else Icons.Rounded.StarOutline,
          contentDescription = stringResource(
            if (item.anime.isFavorite) R.string.detail_favorite_remove else R.string.detail_favorite_add
          ),
          tint = if (item.anime.isFavorite) VlcOrange else TextPrimary
        )
      }

      item.anime.malScore?.let { score ->
        Row(
          modifier = Modifier
            .align(Alignment.TopStart)
            .padding(8.dp)
            .background(Color.Black.copy(alpha = 0.68f), AnimePlayRoleShapes.Poster)
            .padding(horizontal = 6.dp, vertical = 4.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
          Icon(
            imageVector = Icons.Rounded.Star,
            contentDescription = null,
            tint = TextPrimary,
            modifier = Modifier.size(12.dp)
          )
          Text(
            text = String.format(Locale.US, "%.1f", score),
            style = MaterialTheme.typography.labelSmall,
            color = TextPrimary
          )
        }
      }

      if (watchedFraction > 0f) {
        LinearProgressIndicator(
          progress = { watchedFraction },
          color = VlcOrange,
          trackColor = ObsidianSurfaceElevated,
          modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .height(3.dp)
        )
      }
    }

    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
      Text(
        text = item.anime.title,
        style = MaterialTheme.typography.titleMedium,
        color = TextPrimary,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
      )
      val metaText = buildString {
        append(item.anime.genre)
        val dateText = item.anime.releaseDate.ifBlank { item.anime.releaseYear?.toString().orEmpty() }
        if (dateText.isNotBlank()) append(" • $dateText")
      }
      Text(
        text = metaText,
        style = MaterialTheme.typography.labelSmall,
        color = SecondaryGray,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    }
  }
}

@Composable
private fun AnimeListRowCard(
  item: AnimeWithStats,
  onClick: () -> Unit,
  onToggleFavorite: () -> Unit
) {
  val interaction = remember { MutableInteractionSource() }
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .scaleOnPress(interaction)
      .clickable(interactionSource = interaction, indication = null, onClick = onClick)
      .padding(vertical = 6.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    VideoThumbnail(
      uri = item.anime.effectiveCoverUri,
      contentDescription = stringResource(R.string.cd_anime_thumbnail, item.anime.title),
      modifier = Modifier
        .size(width = 64.dp, height = 92.dp)
        .clip(AnimePlayRoleShapes.Poster),
      targetWidthPx = 200,
      targetHeightPx = 300
    )
    Column(
      modifier = Modifier.weight(1f),
      verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
      Text(
        text = item.anime.title,
        style = MaterialTheme.typography.titleMedium,
        color = TextPrimary,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
      )
      Text(
        text = "${item.anime.genre} • ${stringResource(R.string.home_watched_progress, item.watchedCount, item.anime.episodeCount)}",
        style = MaterialTheme.typography.labelSmall,
        color = SecondaryGray,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
      item.anime.malScore?.let { sc ->
        Text(
          text = stringResource(R.string.mal_score_badge, sc),
          style = MaterialTheme.typography.labelSmall,
          color = SecondaryGray
        )
      }
    }
    IconButton(
      onClick = onToggleFavorite,
      modifier = Modifier.minimumInteractiveComponentSize()
    ) {
      Icon(
        imageVector = if (item.anime.isFavorite) Icons.Rounded.Star else Icons.Rounded.StarOutline,
        contentDescription = stringResource(
          if (item.anime.isFavorite) R.string.detail_favorite_remove else R.string.detail_favorite_add
        ),
        tint = if (item.anime.isFavorite) VlcOrange else SecondaryGray
      )
    }
  }
}

@Composable
fun EmptyLibraryState(
  isFiltered: Boolean,
  foldersLabel: String,
  onRefresh: () -> Unit,
  onOpenSettings: () -> Unit,
  onClearSearch: () -> Unit
) {
  val buttonInteraction = remember { MutableInteractionSource() }
  val settingsInteraction = remember { MutableInteractionSource() }
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 24.dp, horizontal = 12.dp),
    contentAlignment = Alignment.Center
  ) {
    Column(
      modifier = Modifier.widthIn(max = 380.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Image(
        painter = painterResource(id = R.drawable.img_empty_cinema),
        contentDescription = stringResource(R.string.cd_empty_illustration),
        contentScale = ContentScale.Crop,
        modifier = Modifier
          .size(136.dp)
          .clip(AnimePlayRoleShapes.Poster)
      )

      Spacer(modifier = Modifier.height(16.dp))

      Text(
        text = when {
          isFiltered -> stringResource(R.string.home_search_empty_title)
          foldersLabel.isBlank() -> stringResource(R.string.home_empty_title_no_folders)
          else -> stringResource(R.string.home_empty_title)
        },
        style = MaterialTheme.typography.titleLarge,
        color = TextPrimary,
        textAlign = TextAlign.Center
      )

      Spacer(modifier = Modifier.height(8.dp))

      Text(
        text = when {
          isFiltered -> stringResource(R.string.home_search_empty_subtitle)
          foldersLabel.isBlank() -> stringResource(R.string.home_empty_subtitle_no_folders)
          else -> stringResource(R.string.home_empty_subtitle, foldersLabel)
        },
        style = MaterialTheme.typography.bodyMedium,
        color = SecondaryGray,
        textAlign = TextAlign.Center
      )

      Spacer(modifier = Modifier.height(20.dp))

      Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        if (!isFiltered) {
          OutlinedButton(
            onClick = onOpenSettings,
            interactionSource = settingsInteraction,
            shape = AnimePlayRoleShapes.Button,
            border = BorderStroke(1.dp, ObsidianBorder),
            modifier = Modifier
              .height(48.dp)
              .scaleOnPress(settingsInteraction)
          ) {
            Text(
              text = stringResource(R.string.home_open_settings),
              color = TextPrimary,
              style = MaterialTheme.typography.labelLarge
            )
          }
        }
        Button(
          onClick = if (isFiltered) onClearSearch else onRefresh,
          interactionSource = buttonInteraction,
          colors = ButtonDefaults.buttonColors(
            containerColor = TextPrimary,
            contentColor = OnPrimaryBlack
          ),
          shape = AnimePlayRoleShapes.Button,
          modifier = Modifier
            .height(48.dp)
            .scaleOnPress(buttonInteraction)
            .testTag("empty_state_action_button")
        ) {
          Text(
            text = stringResource(
              if (isFiltered) R.string.action_clear_search else R.string.action_refresh
            ),
            style = MaterialTheme.typography.labelLarge
          )
        }
      }
    }
  }
}

