package com.example.ui.home

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import com.example.ui.theme.ObsidianBackground
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceElevated
import com.example.ui.theme.OnPrimaryBlack
import com.example.ui.theme.SecondaryGray
import com.example.ui.theme.VlcOrange
import com.example.ui.theme.VlcOrangeSoft
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
    GridCells.Adaptive(minSize = 156.dp)
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .testTag("pustaka_tab")
  ) {
    // Top Search + Sort + Grid/List Controls
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .background(ObsidianSurface.copy(alpha = 0.65f))
        .padding(horizontal = 16.dp, vertical = 10.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp)
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
              tint = VlcOrange
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
          shape = RoundedCornerShape(14.dp),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = VlcOrange,
            unfocusedBorderColor = ObsidianBorder,
            focusedContainerColor = ObsidianSurface,
            unfocusedContainerColor = ObsidianSurface,
            cursorColor = VlcOrange
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
              .clip(RoundedCornerShape(12.dp))
              .background(ObsidianSurface)
              .border(1.dp, ObsidianBorder, RoundedCornerShape(12.dp))
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Rounded.Sort,
              contentDescription = null,
              tint = VlcOrange
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
                    color = if (state.sortOption == option) VlcOrange else MaterialTheme.colorScheme.onSurface
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
            .clip(RoundedCornerShape(12.dp))
            .background(ObsidianSurface)
            .border(1.dp, ObsidianBorder, RoundedCornerShape(12.dp))
        ) {
          Icon(
            imageVector = if (state.isListViewMode) Icons.Rounded.GridView else Icons.AutoMirrored.Rounded.ViewList,
            contentDescription = stringResource(R.string.library_toggle_view),
            tint = MaterialTheme.colorScheme.onSurface
          )
        }
      }

      LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
          FilterChip(
            selected = state.favoritesOnly,
            onClick = onToggleFavoritesOnly,
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
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = VlcOrange,
              selectedLabelColor = OnPrimaryBlack,
              selectedLeadingIconColor = OnPrimaryBlack,
              containerColor = ObsidianSurfaceElevated,
              labelColor = SecondaryGray,
              iconColor = VlcOrange
            )
          )
        }
        item {
          FilterChip(
            selected = state.selectedGenre == null &&
              state.selectedYear == null &&
              state.selectedPlaylistId == null &&
              !state.favoritesOnly,
            onClick = onClearAllFilters,
            label = {
              Text(
                text = stringResource(R.string.home_filter_all_genres),
                style = MaterialTheme.typography.labelSmall
              )
            },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = VlcOrange,
              selectedLabelColor = OnPrimaryBlack,
              containerColor = ObsidianSurfaceElevated,
              labelColor = SecondaryGray
            )
          )
        }
        items(state.availableGenres) { genre ->
          FilterChip(
            selected = state.selectedGenre == genre,
            onClick = { onSelectGenre(genre) },
            label = { Text(text = genre, style = MaterialTheme.typography.labelSmall) },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = VlcOrange,
              selectedLabelColor = OnPrimaryBlack,
              containerColor = ObsidianSurfaceElevated,
              labelColor = SecondaryGray
            )
          )
        }
        items(state.availableYears) { year ->
          FilterChip(
            selected = state.selectedYear == year,
            onClick = { onSelectYear(year) },
            label = { Text(text = year.toString(), style = MaterialTheme.typography.labelSmall) },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = VlcOrange,
              selectedLabelColor = OnPrimaryBlack,
              containerColor = ObsidianSurfaceElevated,
              labelColor = SecondaryGray
            )
          )
        }
      }

      // Custom Playlists filter & creation row
      LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        item {
          FilterChip(
            selected = false,
            onClick = { showCreatePlaylistDialog = true },
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
            colors = FilterChipDefaults.filterChipColors(
              containerColor = VlcOrangeSoft,
              labelColor = VlcOrange,
              iconColor = VlcOrange
            ),
            modifier = Modifier.testTag("create_playlist_chip")
          )
        }
        items(state.playlists, key = { it.id }) { playlist ->
          val isSelected = state.selectedPlaylistId == playlist.id
          FilterChip(
            selected = isSelected,
            onClick = { onSelectPlaylist(playlist.id) },
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
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = VlcOrange,
              selectedLabelColor = OnPrimaryBlack,
              selectedLeadingIconColor = OnPrimaryBlack,
              selectedTrailingIconColor = OnPrimaryBlack,
              containerColor = ObsidianSurfaceElevated,
              labelColor = SecondaryGray,
              iconColor = VlcOrange
            )
          )
        }
      }
    }

    LazyVerticalGrid(
      columns = columns,
      contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 28.dp),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
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
    title = {
      Text(
        text = stringResource(R.string.playlist_create_title),
        style = MaterialTheme.typography.titleLarge
      )
    },
    text = {
      OutlinedTextField(
        value = name,
        onValueChange = { name = it },
        placeholder = { Text(stringResource(R.string.playlist_name_hint)) },
        singleLine = true,
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

  Surface(
    color = ObsidianSurface,
    shape = RoundedCornerShape(16.dp),
    modifier = Modifier
      .fillMaxWidth()
      .scaleOnPress(interaction)
      .border(1.dp, ObsidianBorder, RoundedCornerShape(16.dp))
      .clip(RoundedCornerShape(16.dp))
      .clickable(interactionSource = interaction, indication = null, onClick = onClick)
      .testTag("anime_card_${item.anime.id}")
  ) {
    Column {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .aspectRatio(4f / 3f)
      ) {
        VideoThumbnail(
          uri = item.anime.effectiveCoverUri,
          contentDescription = stringResource(R.string.cd_anime_thumbnail, item.anime.title),
          modifier = Modifier.fillMaxSize(),
          targetWidthPx = 420,
          targetHeightPx = 315
        )
        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(
              Brush.verticalGradient(
                colors = listOf(Color.Transparent, ObsidianBackground.copy(alpha = 0.72f))
              )
            )
        )
        Surface(
          color = ObsidianBackground.copy(alpha = 0.85f),
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(8.dp)
        ) {
          Text(
            text = stringResource(R.string.home_episode_count, item.anime.episodeCount),
            style = MaterialTheme.typography.labelSmall,
            color = VlcOrange,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
          )
        }

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
            tint = if (item.anime.isFavorite) VlcOrange else MaterialTheme.colorScheme.onBackground
          )
        }

        item.anime.malScore?.let { score ->
          Surface(
            color = ObsidianBackground.copy(alpha = 0.85f),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
              .align(Alignment.TopStart)
              .padding(8.dp)
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
              Icon(
                imageVector = Icons.Rounded.Star,
                contentDescription = null,
                tint = VlcOrange,
                modifier = Modifier.size(12.dp)
              )
              Text(
                text = String.format(Locale.US, "%.1f", score),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onBackground
              )
            }
          }
        }
      }

      if (watchedFraction > 0f) {
        LinearProgressIndicator(
          progress = { watchedFraction },
          color = VlcOrange,
          trackColor = ObsidianSurfaceElevated,
          modifier = Modifier
            .fillMaxWidth()
            .height(3.dp)
        )
      }

      Column(
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
      ) {
        Text(
          text = item.anime.title,
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onSurface,
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
}

@Composable
private fun AnimeListRowCard(
  item: AnimeWithStats,
  onClick: () -> Unit,
  onToggleFavorite: () -> Unit
) {
  val interaction = remember { MutableInteractionSource() }
  Surface(
    color = ObsidianSurface,
    shape = RoundedCornerShape(14.dp),
    modifier = Modifier
      .fillMaxWidth()
      .scaleOnPress(interaction)
      .border(1.dp, ObsidianBorder, RoundedCornerShape(14.dp))
      .clip(RoundedCornerShape(14.dp))
      .clickable(interactionSource = interaction, indication = null, onClick = onClick)
  ) {
    Row(
      modifier = Modifier.padding(10.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      VideoThumbnail(
        uri = item.anime.effectiveCoverUri,
        contentDescription = stringResource(R.string.cd_anime_thumbnail, item.anime.title),
        modifier = Modifier
          .size(width = 96.dp, height = 72.dp)
          .clip(RoundedCornerShape(10.dp)),
        targetWidthPx = 280,
        targetHeightPx = 210
      )
      Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(4.dp)
      ) {
        Text(
          text = item.anime.title,
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onSurface,
          maxLines = 1,
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
            color = VlcOrange
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
      .padding(vertical = 20.dp, horizontal = 12.dp),
    contentAlignment = Alignment.Center
  ) {
    Column(
      modifier = Modifier.widthIn(max = 380.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Box(
        modifier = Modifier
          .size(136.dp)
          .clip(RoundedCornerShape(24.dp))
          .border(1.dp, ObsidianBorder, RoundedCornerShape(24.dp))
      ) {
        Image(
          painter = painterResource(id = R.drawable.img_empty_cinema),
          contentDescription = stringResource(R.string.cd_empty_illustration),
          contentScale = ContentScale.Crop,
          modifier = Modifier.fillMaxSize()
        )
        Box(
          modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(10.dp)
            .size(36.dp)
            .clip(CircleShape)
            .background(VlcOrangeSoft),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Rounded.VideoLibrary,
            contentDescription = null,
            tint = VlcOrange,
            modifier = Modifier.size(18.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      Text(
        text = stringResource(
          if (isFiltered) R.string.home_search_empty_title else R.string.home_empty_title
        ),
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onBackground,
        textAlign = TextAlign.Center
      )

      Spacer(modifier = Modifier.height(8.dp))

      Text(
        text = if (isFiltered) {
          stringResource(R.string.home_search_empty_subtitle)
        } else {
          stringResource(R.string.home_empty_subtitle, foldersLabel)
        },
        style = MaterialTheme.typography.bodyMedium,
        color = SecondaryGray,
        textAlign = TextAlign.Center
      )

      Spacer(modifier = Modifier.height(16.dp))

      Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        if (!isFiltered) {
          OutlinedButton(
            onClick = onOpenSettings,
            interactionSource = settingsInteraction,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
              .height(48.dp)
              .scaleOnPress(settingsInteraction)
          ) {
            Text(
              text = stringResource(R.string.home_open_settings),
              color = VlcOrange,
              style = MaterialTheme.typography.labelLarge
            )
          }
        }
        Button(
          onClick = if (isFiltered) onClearSearch else onRefresh,
          interactionSource = buttonInteraction,
          colors = ButtonDefaults.buttonColors(
            containerColor = VlcOrange,
            contentColor = OnPrimaryBlack
          ),
          shape = RoundedCornerShape(14.dp),
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
