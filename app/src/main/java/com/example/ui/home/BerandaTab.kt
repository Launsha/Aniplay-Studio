package com.example.ui.home

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.entity.AnimeWithStats
import com.example.data.local.entity.EpisodeWithAnime
import com.example.ui.components.VideoThumbnail
import com.example.ui.theme.AnimePlayRoleShapes
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianDivider
import com.example.ui.theme.ObsidianSurfaceElevated
import com.example.ui.theme.OnPrimaryBlack
import com.example.ui.theme.SecondaryGray
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.VlcOrange
import com.example.util.TimeFormatters
import com.example.util.scaleOnPress
import java.util.Calendar
import java.util.Locale

@Composable
fun BerandaTab(
  state: HomeUiState,
  onAnimeClick: (Long) -> Unit,
  onEpisodeClick: (Long) -> Unit,
  onOpenLibraryTab: () -> Unit,
  onOpenSettingsTab: () -> Unit,
  onOpenGuide: () -> Unit,
  onRefresh: () -> Unit
) {
  LazyColumn(
    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 32.dp),
    verticalArrangement = Arrangement.spacedBy(24.dp),
    modifier = Modifier
      .fillMaxSize()
      .testTag("beranda_tab")
  ) {
    // 1. Greeting header (open layout, no card box or gradient)
    item {
      WelcomeGreetingSection(
        configuredFoldersLabel = state.configuredFoldersLabel,
        watchStreakDays = state.watchStreakDays,
        onOpenGuide = onOpenGuide,
        onOpenSettings = onOpenSettingsTab
      )
    }

    // 2. Summary metrics row
    item {
      SummaryMetricsBanner(
        totalWatchedMs = state.totalWatchedMs,
        watchedEpisodes = state.totalWatchedEpisodes,
        animeCount = state.totalLibraryCount
      )
    }

    // 3. Continue Watching
    if (state.continueWatching.isNotEmpty()) {
      item {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
          Text(
            text = stringResource(R.string.home_continue_watching),
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary
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
        }
      }
    }

    // 4. Favorites Showcase
    if (state.favoriteAnime.isNotEmpty()) {
      item {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
          Text(
            text = stringResource(R.string.home_favorites_title),
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary
          )
          LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(state.favoriteAnime, key = { it.anime.id }) { item ->
              CompactAnimeShowcaseCard(
                item = item,
                onClick = { onAnimeClick(item.anime.id) }
              )
            }
          }
        }
      }
    }

    // 5. Recent Library or actionable empty state
    if (state.allLibrary.isNotEmpty()) {
      item {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = stringResource(R.string.home_recent_title),
              style = MaterialTheme.typography.titleMedium,
              color = TextPrimary
            )
            TextButton(onClick = onOpenLibraryTab) {
              Text(
                text = stringResource(R.string.home_view_all_library),
                style = MaterialTheme.typography.labelLarge,
                color = SecondaryGray
              )
            }
          }
          LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(state.allLibrary.take(10), key = { it.anime.id }) { item ->
              CompactAnimeShowcaseCard(
                item = item,
                onClick = { onAnimeClick(item.anime.id) }
              )
            }
          }
        }
      }
    } else if (state.hasScannedOnce && !state.isScanning) {
      item {
        EmptyLibraryState(
          isFiltered = false,
          foldersLabel = state.configuredFoldersLabel,
          onRefresh = onRefresh,
          onOpenSettings = onOpenSettingsTab,
          onClearSearch = {}
        )
      }
    }

    // 6. Quick Start & Tips
    item {
      Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        HorizontalDivider(color = ObsidianDivider)
        QuickStartGuideSection(
          onOpenGuide = onOpenGuide,
          onOpenSettings = onOpenSettingsTab,
          onRefresh = onRefresh,
          isScanning = state.isScanning
        )
      }
    }
  }
}

@Composable
private fun WelcomeGreetingSection(
  configuredFoldersLabel: String,
  watchStreakDays: Int,
  onOpenGuide: () -> Unit,
  onOpenSettings: () -> Unit
) {
  val hour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
  val greetingRes = when (hour) {
    in 4..10 -> R.string.greeting_morning
    in 11..14 -> R.string.greeting_afternoon
    in 15..18 -> R.string.greeting_evening
    else -> R.string.greeting_night
  }

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("welcome_greeting_card"),
    verticalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = stringResource(greetingRes),
        style = MaterialTheme.typography.headlineMedium,
        color = TextPrimary,
        modifier = Modifier.weight(1f)
      )
      OutlinedButton(
        onClick = onOpenGuide,
        shape = AnimePlayRoleShapes.Button,
        border = BorderStroke(1.dp, ObsidianBorder),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
        modifier = Modifier.height(40.dp)
      ) {
        Text(
          text = stringResource(R.string.action_open_guide),
          style = MaterialTheme.typography.labelMedium,
          color = TextPrimary
        )
      }
    }

    Text(
      text = stringResource(R.string.greeting_welcome_subtitle),
      style = MaterialTheme.typography.bodyMedium,
      color = SecondaryGray
    )

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(
        modifier = Modifier
          .weight(1f, fill = false)
          .clickable(onClick = onOpenSettings)
          .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        Icon(
          imageVector = Icons.Rounded.FolderOpen,
          contentDescription = null,
          tint = SecondaryGray,
          modifier = Modifier.size(16.dp)
        )
        Text(
          text = if (configuredFoldersLabel.isBlank()) {
            stringResource(R.string.home_no_folders_configured)
          } else {
            stringResource(R.string.home_configured_folders_chip, configuredFoldersLabel)
          },
          style = MaterialTheme.typography.labelMedium,
          color = TextPrimary,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }

      if (watchStreakDays > 0) {
        Text(
          text = stringResource(R.string.home_streak_badge, watchStreakDays),
          style = MaterialTheme.typography.labelMedium,
          color = SecondaryGray
        )
      }
    }
  }
}

@Composable
fun SummaryMetricsBanner(
  totalWatchedMs: Long,
  watchedEpisodes: Int,
  animeCount: Int
) {
  val totalHours = totalWatchedMs / 3_600_000.0
  val hoursDisplay = if (totalHours >= 1.0 || totalWatchedMs == 0L) {
    stringResource(R.string.home_hours_value, totalHours)
  } else {
    val mins = (totalWatchedMs / 60_000L).coerceAtLeast(1L).toInt()
    stringResource(R.string.home_minutes_value, mins)
  }

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("home_summary_banner"),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    HorizontalDivider(color = ObsidianDivider)
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 4.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      SummaryStatColumn(
        value = hoursDisplay,
        label = stringResource(R.string.home_total_hours_label),
        modifier = Modifier.weight(1.1f)
      )
      VerticalDivider(
        color = ObsidianDivider,
        modifier = Modifier.height(34.dp)
      )
      SummaryStatColumn(
        value = watchedEpisodes.toString(),
        label = stringResource(R.string.home_episodes_watched_label),
        modifier = Modifier.weight(1f)
      )
      VerticalDivider(
        color = ObsidianDivider,
        modifier = Modifier.height(34.dp)
      )
      SummaryStatColumn(
        value = animeCount.toString(),
        label = stringResource(R.string.home_anime_count_label),
        modifier = Modifier.weight(1f)
      )
    }
    HorizontalDivider(color = ObsidianDivider)
  }
}

@Composable
private fun SummaryStatColumn(
  value: String,
  label: String,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier.padding(horizontal = 8.dp),
    verticalArrangement = Arrangement.spacedBy(2.dp)
  ) {
    Text(
      text = value,
      style = MaterialTheme.typography.titleLarge,
      color = TextPrimary,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis
    )
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall,
      color = SecondaryGray,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis
    )
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

  Column(
    modifier = Modifier
      .width(240.dp)
      .scaleOnPress(interaction)
      .clickable(interactionSource = interaction, indication = null, onClick = onClick)
      .testTag("continue_card_${item.episode.id}"),
    verticalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .aspectRatio(16f / 9f)
        .clip(AnimePlayRoleShapes.Poster)
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
          .background(Color.Black.copy(alpha = 0.25f))
      )
      Box(
        modifier = Modifier
          .size(44.dp)
          .align(Alignment.Center)
          .clip(CircleShape)
          .background(VlcOrange),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.Rounded.PlayArrow,
          contentDescription = stringResource(R.string.player_play),
          tint = OnPrimaryBlack,
          modifier = Modifier.size(26.dp)
        )
      }
      Text(
        text = "${TimeFormatters.formatDuration(item.episode.lastPositionMs)} / ${
          TimeFormatters.formatDuration(item.episode.durationMs)
        }",
        style = MaterialTheme.typography.labelSmall,
        color = TextPrimary,
        modifier = Modifier
          .align(Alignment.BottomEnd)
          .padding(8.dp)
          .background(Color.Black.copy(alpha = 0.65f), AnimePlayRoleShapes.Poster)
          .padding(horizontal = 6.dp, vertical = 2.dp)
      )
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

    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
      Text(
        text = item.anime.title,
        style = MaterialTheme.typography.titleSmall,
        color = TextPrimary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
      Text(
        text = stringResource(R.string.home_episode_badge, item.episode.number),
        style = MaterialTheme.typography.labelSmall,
        color = SecondaryGray,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    }
  }
}

@Composable
private fun CompactAnimeShowcaseCard(
  item: AnimeWithStats,
  onClick: () -> Unit
) {
  val interaction = remember { MutableInteractionSource() }
  Column(
    modifier = Modifier
      .width(136.dp)
      .scaleOnPress(interaction)
      .clickable(interactionSource = interaction, indication = null, onClick = onClick),
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
        targetWidthPx = 280,
        targetHeightPx = 420
      )
      item.anime.malScore?.let { score ->
        Row(
          modifier = Modifier
            .align(Alignment.TopStart)
            .padding(6.dp)
            .background(Color.Black.copy(alpha = 0.68f), AnimePlayRoleShapes.Poster)
            .padding(horizontal = 6.dp, vertical = 3.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
          Icon(
            imageVector = Icons.Rounded.Star,
            contentDescription = null,
            tint = TextPrimary,
            modifier = Modifier.size(11.dp)
          )
          Text(
            text = String.format(Locale.US, "%.1f", score),
            style = MaterialTheme.typography.labelSmall,
            color = TextPrimary
          )
        }
      }
    }

    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
      Text(
        text = item.anime.title,
        style = MaterialTheme.typography.titleSmall,
        color = TextPrimary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
      Text(
        text = stringResource(R.string.home_episode_count, item.anime.episodeCount),
        style = MaterialTheme.typography.labelSmall,
        color = SecondaryGray
      )
    }
  }
}

@Composable
private fun QuickStartGuideSection(
  onOpenGuide: () -> Unit,
  onOpenSettings: () -> Unit,
  onRefresh: () -> Unit,
  isScanning: Boolean
) {
  Column(
    modifier = Modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    Text(
      text = stringResource(R.string.home_quick_guide_title),
      style = MaterialTheme.typography.titleMedium,
      color = TextPrimary
    )
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
      Text(
        text = stringResource(R.string.home_quick_guide_step1),
        style = MaterialTheme.typography.bodySmall,
        color = SecondaryGray
      )
      Text(
        text = stringResource(R.string.home_quick_guide_step2),
        style = MaterialTheme.typography.bodySmall,
        color = SecondaryGray
      )
      Text(
        text = stringResource(R.string.home_quick_guide_step3),
        style = MaterialTheme.typography.bodySmall,
        color = SecondaryGray
      )
    }

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      val settingsInteraction = remember { MutableInteractionSource() }
      OutlinedButton(
        onClick = onOpenSettings,
        interactionSource = settingsInteraction,
        shape = AnimePlayRoleShapes.Button,
        border = BorderStroke(1.dp, ObsidianBorder),
        modifier = Modifier
          .weight(1f)
          .height(48.dp)
          .scaleOnPress(settingsInteraction)
      ) {
        Text(
          text = stringResource(R.string.home_open_settings),
          color = TextPrimary,
          style = MaterialTheme.typography.labelLarge,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }

      val refreshInteraction = remember { MutableInteractionSource() }
      Button(
        onClick = onRefresh,
        enabled = !isScanning,
        interactionSource = refreshInteraction,
        colors = ButtonDefaults.buttonColors(
          containerColor = TextPrimary,
          contentColor = OnPrimaryBlack
        ),
        shape = AnimePlayRoleShapes.Button,
        modifier = Modifier
          .weight(1f)
          .height(48.dp)
          .scaleOnPress(refreshInteraction)
      ) {
        Text(
          text = stringResource(R.string.action_refresh),
          style = MaterialTheme.typography.labelLarge,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }
    }

    TextButton(
      onClick = onOpenGuide,
      modifier = Modifier.align(Alignment.End)
    ) {
      Text(
        text = stringResource(R.string.guide_screen_title),
        style = MaterialTheme.typography.labelMedium,
        color = SecondaryGray
      )
    }
  }
}

