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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 28.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
    modifier = Modifier
      .fillMaxSize()
      .testTag("beranda_tab")
  ) {
    // 1. Personalized Welcome Banner with Greeting & Panduan button
    item {
      WelcomeGreetingCard(
        configuredFoldersLabel = state.configuredFoldersLabel,
        watchStreakDays = state.watchStreakDays,
        onOpenGuide = onOpenGuide,
        onOpenSettings = onOpenSettingsTab
      )
    }

    // 2. Summary Metrics Banner
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
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
            text = stringResource(R.string.home_continue_watching),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
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
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
            text = stringResource(R.string.home_favorites_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
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

    // 5. Recent Library / Quick Start Guide if empty
    if (state.allLibrary.isNotEmpty()) {
      item {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = stringResource(R.string.home_recent_title),
              style = MaterialTheme.typography.titleMedium,
              color = MaterialTheme.colorScheme.onBackground
            )
            TextButton(onClick = onOpenLibraryTab) {
              Text(
                text = stringResource(R.string.home_view_all_library),
                style = MaterialTheme.typography.labelMedium,
                color = VlcOrange
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

    // 6. Quick Start & Tips Card (always keeps Home warm & helpful)
    item {
      QuickStartGuideCard(
        onOpenGuide = onOpenGuide,
        onOpenSettings = onOpenSettingsTab,
        onRefresh = onRefresh,
        isScanning = state.isScanning
      )
    }
  }
}

@Composable
private fun WelcomeGreetingCard(
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

  Surface(
    color = ObsidianSurface,
    shape = RoundedCornerShape(20.dp),
    modifier = Modifier
      .fillMaxWidth()
      .border(1.dp, ObsidianBorder, RoundedCornerShape(20.dp))
      .testTag("welcome_greeting_card")
  ) {
    Box {
      Image(
        painter = painterResource(id = R.drawable.img_welcome_hero),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        alpha = 0.22f,
        modifier = Modifier.matchParentSize()
      )
      Box(
        modifier = Modifier
          .matchParentSize()
          .background(
            Brush.horizontalGradient(
              colors = listOf(
                ObsidianBackground.copy(alpha = 0.92f),
                ObsidianBackground.copy(alpha = 0.72f)
              )
            )
          )
      )
      Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = stringResource(greetingRes),
            style = MaterialTheme.typography.titleLarge,
            color = VlcOrange
          )
          OutlinedButton(
            onClick = onOpenGuide,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            shape = RoundedCornerShape(50),
            modifier = Modifier.height(36.dp)
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Rounded.HelpOutline,
              contentDescription = null,
              tint = VlcOrange,
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = stringResource(R.string.action_open_guide),
              style = MaterialTheme.typography.labelMedium,
              color = VlcOrange
            )
          }
        }
        Text(
          text = stringResource(R.string.greeting_welcome_subtitle),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurface
        )
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Surface(
            color = ObsidianSurfaceElevated.copy(alpha = 0.9f),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
              .weight(1f, fill = false)
              .clip(RoundedCornerShape(10.dp))
              .clickable(onClick = onOpenSettings)
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              Icon(
                imageVector = Icons.Rounded.FolderOpen,
                contentDescription = null,
                tint = VlcOrange,
                modifier = Modifier.size(15.dp)
              )
              Text(
                text = stringResource(R.string.home_configured_folders_chip, configuredFoldersLabel),
                style = MaterialTheme.typography.labelSmall,
                color = SecondaryGray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }
          }
          if (watchStreakDays > 0) {
            Surface(
              color = VlcOrangeSoft,
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.border(1.dp, VlcOrange.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
            ) {
              Text(
                text = stringResource(R.string.home_streak_badge, watchStreakDays),
                style = MaterialTheme.typography.labelSmall,
                color = VlcOrange,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
              )
            }
          }
        }
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

  Surface(
    color = ObsidianSurface,
    shape = RoundedCornerShape(18.dp),
    modifier = Modifier
      .fillMaxWidth()
      .border(1.dp, ObsidianBorder, RoundedCornerShape(18.dp))
      .testTag("home_summary_banner")
  ) {
    Column(
      modifier = Modifier
        .background(
          Brush.horizontalGradient(
            colors = listOf(VlcOrangeSoft.copy(alpha = 0.25f), Color.Transparent)
          )
        )
        .padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      Text(
        text = stringResource(R.string.home_summary_title),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface
      )
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        SummaryStatPill(
          icon = Icons.Rounded.Schedule,
          value = hoursDisplay,
          label = stringResource(R.string.home_total_hours_label),
          accent = true,
          modifier = Modifier.weight(1.15f)
        )
        SummaryStatPill(
          icon = Icons.Rounded.CheckCircle,
          value = watchedEpisodes.toString(),
          label = stringResource(R.string.home_episodes_watched_label),
          accent = false,
          modifier = Modifier.weight(0.95f)
        )
        SummaryStatPill(
          icon = Icons.Rounded.Movie,
          value = animeCount.toString(),
          label = stringResource(R.string.home_anime_count_label),
          accent = false,
          modifier = Modifier.weight(0.9f)
        )
      }
    }
  }
}

@Composable
private fun SummaryStatPill(
  icon: ImageVector,
  value: String,
  label: String,
  accent: Boolean,
  modifier: Modifier = Modifier
) {
  Surface(
    color = if (accent) VlcOrangeSoft else ObsidianSurfaceElevated,
    shape = RoundedCornerShape(12.dp),
    modifier = modifier.border(
      1.dp,
      if (accent) VlcOrange.copy(alpha = 0.45f) else ObsidianBorder,
      RoundedCornerShape(12.dp)
    )
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
      verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = if (accent) VlcOrange else SecondaryGray,
        modifier = Modifier.size(18.dp)
      )
      Text(
        text = value,
        style = MaterialTheme.typography.titleMedium,
        color = if (accent) VlcOrange else MaterialTheme.colorScheme.onSurface,
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
private fun CompactAnimeShowcaseCard(
  item: AnimeWithStats,
  onClick: () -> Unit
) {
  val interaction = remember { MutableInteractionSource() }
  Surface(
    color = ObsidianSurface,
    shape = RoundedCornerShape(14.dp),
    modifier = Modifier
      .width(156.dp)
      .scaleOnPress(interaction)
      .border(1.dp, ObsidianBorder, RoundedCornerShape(14.dp))
      .clip(RoundedCornerShape(14.dp))
      .clickable(interactionSource = interaction, indication = null, onClick = onClick)
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
          targetWidthPx = 360,
          targetHeightPx = 270
        )
        item.anime.malScore?.let { score ->
          Surface(
            color = ObsidianBackground.copy(alpha = 0.85f),
            shape = RoundedCornerShape(6.dp),
            modifier = Modifier
              .align(Alignment.TopStart)
              .padding(6.dp)
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
              Icon(
                imageVector = Icons.Rounded.Star,
                contentDescription = null,
                tint = VlcOrange,
                modifier = Modifier.size(11.dp)
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
      Column(
        modifier = Modifier.padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
      ) {
        Text(
          text = item.anime.title,
          style = MaterialTheme.typography.titleSmall,
          color = MaterialTheme.colorScheme.onSurface,
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
}

@Composable
private fun QuickStartGuideCard(
  onOpenGuide: () -> Unit,
  onOpenSettings: () -> Unit,
  onRefresh: () -> Unit,
  isScanning: Boolean
) {
  Surface(
    color = ObsidianSurface,
    shape = RoundedCornerShape(18.dp),
    modifier = Modifier
      .fillMaxWidth()
      .border(1.dp, ObsidianBorder, RoundedCornerShape(18.dp))
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      Text(
        text = stringResource(R.string.home_quick_guide_title),
        style = MaterialTheme.typography.titleMedium,
        color = VlcOrange
      )
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
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        OutlinedButton(
          onClick = onOpenSettings,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .weight(1f)
            .height(46.dp)
        ) {
          Text(
            text = stringResource(R.string.home_open_settings),
            color = VlcOrange,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }
        Button(
          onClick = onRefresh,
          enabled = !isScanning,
          colors = ButtonDefaults.buttonColors(
            containerColor = VlcOrange,
            contentColor = OnPrimaryBlack
          ),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .weight(1f)
            .height(46.dp)
        ) {
          Icon(
            imageVector = Icons.Rounded.Refresh,
            contentDescription = null,
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = stringResource(R.string.action_refresh),
            style = MaterialTheme.typography.labelMedium,
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
          color = VlcOrange
        )
      }
    }
  }
}
