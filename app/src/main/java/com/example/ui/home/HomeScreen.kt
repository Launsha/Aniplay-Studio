package com.example.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.local.entity.AnimeWithStats
import com.example.data.local.entity.EpisodeWithAnime
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
  onAnimeClick: (Long) -> Unit,
  onEpisodeClick: (Long) -> Unit,
  viewModel: HomeViewModel = hiltViewModel()
) {
  val state by viewModel.uiState.collectAsStateWithLifecycle()

  LaunchedEffect(Unit) {
    viewModel.scanIfNeeded()
  }

  val infiniteTransition = rememberInfiniteTransition(label = "refresh_spin")
  val spinAngle by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 360f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 900, easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "spin_angle"
  )

  Scaffold(
    containerColor = ObsidianBackground,
    contentWindowInsets = WindowInsets.statusBars,
    topBar = {
      TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = ObsidianBackground.copy(alpha = 0.96f),
          titleContentColor = MaterialTheme.colorScheme.onBackground
        ),
        title = {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Image(
              painter = painterResource(id = R.drawable.img_app_icon_play),
              contentDescription = stringResource(R.string.cd_app_logo),
              contentScale = ContentScale.Crop,
              modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .border(1.dp, ObsidianBorder, RoundedCornerShape(10.dp))
            )
            Text(
              text = stringResource(R.string.app_name),
              style = MaterialTheme.typography.titleLarge
            )
          }
        },
        actions = {
          IconButton(
            onClick = { viewModel.toggleSearchExpanded() },
            modifier = Modifier
              .minimumInteractiveComponentSize()
              .testTag("search_toggle_button")
          ) {
            Icon(
              imageVector = if (state.isSearchExpanded) Icons.Rounded.Close else Icons.Rounded.Search,
              contentDescription = stringResource(R.string.action_search),
              tint = if (state.isSearchExpanded) VlcOrange else MaterialTheme.colorScheme.onBackground
            )
          }
          IconButton(
            onClick = { viewModel.refreshLibrary() },
            enabled = !state.isScanning,
            modifier = Modifier
              .minimumInteractiveComponentSize()
              .testTag("refresh_button")
          ) {
            Icon(
              imageVector = Icons.Rounded.Refresh,
              contentDescription = stringResource(R.string.action_refresh),
              tint = if (state.isScanning) VlcOrange else MaterialTheme.colorScheme.onBackground,
              modifier = Modifier.rotate(if (state.isScanning) spinAngle else 0f)
            )
          }
        }
      )
    }
  ) { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
      if (state.isScanning) {
        LinearProgressIndicator(
          color = VlcOrange,
          trackColor = ObsidianSurface,
          modifier = Modifier
            .fillMaxWidth()
            .height(3.dp)
        )
      }

      AnimatedVisibility(visible = state.isSearchExpanded) {
        SearchAndFilterSection(
          query = state.searchQuery,
          onQueryChange = viewModel::onSearchQueryChange,
          genres = state.availableGenres,
          selectedGenre = state.selectedGenre,
          onSelectGenre = viewModel::onSelectGenre,
          years = state.availableYears,
          selectedYear = state.selectedYear,
          onSelectYear = viewModel::onSelectYear
        )
      }

      if (state.scanError != null) {
        Surface(
          color = MaterialTheme.colorScheme.error.copy(alpha = 0.14f),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .border(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
        ) {
          Text(
            text = stringResource(R.string.home_scan_error, state.scanError ?: ""),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(12.dp)
          )
        }
      }

      val isFiltering = state.searchQuery.isNotBlank() ||
        state.selectedGenre != null ||
        state.selectedYear != null

      if (state.animeList.isEmpty() && state.hasScannedOnce && !state.isScanning) {
        EmptyLibraryState(
          isFiltered = isFiltering,
          onRefresh = { viewModel.refreshLibrary() },
          onClearSearch = { viewModel.toggleSearchExpanded() }
        )
      } else {
        LazyVerticalGrid(
          columns = GridCells.Adaptive(minSize = 156.dp),
          contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
          horizontalArrangement = Arrangement.spacedBy(12.dp),
          verticalArrangement = Arrangement.spacedBy(16.dp),
          modifier = Modifier
            .fillMaxSize()
            .testTag("anime_grid")
        ) {
          if (!isFiltering && state.continueWatching.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
              Column {
                Text(
                  text = stringResource(R.string.home_continue_watching),
                  style = MaterialTheme.typography.titleMedium,
                  color = MaterialTheme.colorScheme.onBackground,
                  modifier = Modifier.padding(vertical = 8.dp)
                )
                LazyRow(
                  horizontalArrangement = Arrangement.spacedBy(12.dp),
                  modifier = Modifier.testTag("continue_watching_row")
                ) {
                  items(state.continueWatching, key = { it.episode.id }) { item ->
                    ContinueWatchingCard(
                      item = item,
                      onClick = { onEpisodeClick(item.episode.id) }
                    )
                  }
                }
                Spacer(modifier = Modifier.height(12.dp))
              }
            }
          }

          if (state.animeList.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
              Text(
                text = stringResource(R.string.home_library_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
              )
            }
          }

          items(state.animeList, key = { it.anime.id }) { animeItem ->
            AnimeGridCard(
              item = animeItem,
              onClick = { onAnimeClick(animeItem.anime.id) }
            )
          }
        }
      }
    }
  }
}

@Composable
private fun SearchAndFilterSection(
  query: String,
  onQueryChange: (String) -> Unit,
  genres: List<String>,
  selectedGenre: String?,
  onSelectGenre: (String?) -> Unit,
  years: List<Int>,
  selectedYear: Int?,
  onSelectYear: (Int?) -> Unit
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .background(ObsidianSurface.copy(alpha = 0.65f))
      .padding(horizontal = 16.dp, vertical = 10.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    OutlinedTextField(
      value = query,
      onValueChange = onQueryChange,
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
        if (query.isNotEmpty()) {
          IconButton(onClick = { onQueryChange("") }) {
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
        .fillMaxWidth()
        .testTag("search_input")
    )

    if (genres.isNotEmpty() || years.isNotEmpty()) {
      LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        item {
          FilterChip(
            selected = selectedGenre == null && selectedYear == null,
            onClick = {
              onSelectGenre(null)
              onSelectYear(null)
            },
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
        items(genres) { genre ->
          FilterChip(
            selected = selectedGenre == genre,
            onClick = { onSelectGenre(genre) },
            label = {
              Text(
                text = genre,
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
        items(years) { year ->
          FilterChip(
            selected = selectedYear == year,
            onClick = { onSelectYear(year) },
            label = {
              Text(
                text = year.toString(),
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
      }
    }
  }
}

@Composable
private fun ContinueWatchingCard(
  item: EpisodeWithAnime,
  onClick: () -> Unit
) {
  val interaction = remember { MutableInteractionSource() }
  val progress = if (item.episode.durationMs > 0L) {
    (item.episode.lastPositionMs.toFloat() / item.episode.durationMs.toFloat()).coerceIn(0f, 1f)
  } else {
    0f
  }

  Surface(
    color = ObsidianSurface,
    shape = RoundedCornerShape(16.dp),
    modifier = Modifier
      .width(236.dp)
      .scaleOnPress(interaction)
      .border(1.dp, ObsidianBorder, RoundedCornerShape(16.dp))
      .clip(RoundedCornerShape(16.dp))
      .clickable(interactionSource = interaction, indication = null, onClick = onClick)
      .testTag("continue_card_${item.episode.id}")
  ) {
    Column {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .aspectRatio(16f / 9f)
      ) {
        VideoThumbnail(
          uri = item.episode.fileUri,
          contentDescription = stringResource(R.string.cd_anime_thumbnail, item.anime.title),
          modifier = Modifier.fillMaxSize(),
          targetWidthPx = 480,
          targetHeightPx = 270
        )
        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(
              Brush.verticalGradient(
                colors = listOf(Color.Transparent, ObsidianBackground.copy(alpha = 0.85f))
              )
            )
        )
        Box(
          modifier = Modifier
            .size(40.dp)
            .align(Alignment.Center)
            .clip(CircleShape)
            .background(VlcOrange.copy(alpha = 0.92f)),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Rounded.PlayArrow,
            contentDescription = stringResource(R.string.player_play),
            tint = OnPrimaryBlack,
            modifier = Modifier.size(24.dp)
          )
        }
        Surface(
          color = ObsidianBackground.copy(alpha = 0.82f),
          shape = RoundedCornerShape(6.dp),
          modifier = Modifier
            .align(Alignment.TopStart)
            .padding(8.dp)
        ) {
          Text(
            text = stringResource(R.string.home_episode_badge, item.episode.number),
            style = MaterialTheme.typography.labelSmall,
            color = VlcOrange,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
          )
        }
        Text(
          text = "${TimeFormatters.formatDuration(item.episode.lastPositionMs)} / ${
            TimeFormatters.formatDuration(item.episode.durationMs)
          }",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onBackground,
          modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(horizontal = 8.dp, vertical = 6.dp)
        )
      }

      LinearProgressIndicator(
        progress = { progress },
        color = VlcOrange,
        trackColor = ObsidianSurfaceElevated,
        modifier = Modifier
          .fillMaxWidth()
          .height(3.dp)
      )

      Text(
        text = item.anime.title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
      )
    }
  }
}

@Composable
private fun AnimeGridCard(
  item: AnimeWithStats,
  onClick: () -> Unit
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
          uri = item.anime.coverUri,
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
          item.anime.releaseYear?.let { append(" • $it") }
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
private fun EmptyLibraryState(
  isFiltered: Boolean,
  onRefresh: () -> Unit,
  onClearSearch: () -> Unit
) {
  val buttonInteraction = remember { MutableInteractionSource() }
  Box(
    modifier = Modifier
      .fillMaxSize()
      .padding(24.dp),
    contentAlignment = Alignment.Center
  ) {
    Column(
      modifier = Modifier.widthIn(max = 380.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Box(
        modifier = Modifier
          .size(160.dp)
          .clip(RoundedCornerShape(28.dp))
          .border(1.dp, ObsidianBorder, RoundedCornerShape(28.dp))
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
            .size(38.dp)
            .clip(CircleShape)
            .background(VlcOrangeSoft),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Rounded.VideoLibrary,
            contentDescription = null,
            tint = VlcOrange,
            modifier = Modifier.size(20.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(20.dp))

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
        text = stringResource(
          if (isFiltered) R.string.home_search_empty_subtitle else R.string.home_empty_subtitle
        ),
        style = MaterialTheme.typography.bodyMedium,
        color = SecondaryGray,
        textAlign = TextAlign.Center
      )

      Spacer(modifier = Modifier.height(22.dp))

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
