package com.example.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.entity.EpisodeWithAnime
import com.example.ui.components.VideoThumbnail
import com.example.ui.theme.AnimePlayRoleShapes
import com.example.ui.theme.ObsidianDivider
import com.example.ui.theme.ObsidianSurfaceElevated
import com.example.ui.theme.SecondaryGray
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.VlcOrange
import com.example.util.TimeFormatters
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.compose.cartesian.data.columnSeries
import com.patrykandpatrick.vico.compose.cartesian.layer.ColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.common.Fill
import com.patrykandpatrick.vico.compose.common.ProvideVicoTheme
import com.patrykandpatrick.vico.compose.common.component.rememberLineComponent
import com.patrykandpatrick.vico.compose.m3.common.rememberM3VicoTheme
import kotlin.math.roundToInt

@Composable
fun StatistikTab(
  state: HomeUiState,
  onEpisodeClick: (Long) -> Unit
) {
  LazyColumn(
    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 32.dp),
    verticalArrangement = Arrangement.spacedBy(24.dp),
    modifier = Modifier
      .fillMaxSize()
      .testTag("statistik_tab")
  ) {
    item {
      SummaryMetricsBanner(
        totalWatchedMs = state.totalWatchedMs,
        watchedEpisodes = state.totalWatchedEpisodes,
        animeCount = state.totalLibraryCount
      )
    }

    item {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .testTag("stats_streak_card"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Icon(
            imageVector = Icons.Rounded.LocalFireDepartment,
            contentDescription = null,
            tint = TextPrimary,
            modifier = Modifier.size(22.dp)
          )
          Text(
            text = stringResource(R.string.stats_streak_label),
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary
          )
        }
        Text(
          text = stringResource(R.string.stats_streak_days, state.watchStreakDays),
          style = MaterialTheme.typography.titleMedium,
          color = TextPrimary
        )
      }
    }

    item {
      Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        HorizontalDivider(color = ObsidianDivider)
        AnimeStatisticsSection(weeklyStats = state.weeklyStats)
      }
    }

    item {
      Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        HorizontalDivider(color = ObsidianDivider)
        AchievementsSection(
          achievements = state.achievements,
          completedAnimeCount = state.completedAnimeCount
        )
      }
    }

    if (state.totalLibraryCount > 0) {
      item {
        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
          HorizontalDivider(color = ObsidianDivider)
          MalStatusBreakdownSection(
            totalCount = state.totalLibraryCount,
            breakdown = state.malStatusBreakdown
          )
        }
      }
    }

    item {
      Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        HorizontalDivider(color = ObsidianDivider)
        Text(
          text = stringResource(R.string.stats_recent_history_title),
          style = MaterialTheme.typography.titleMedium,
          color = TextPrimary
        )

        if (state.recentHistory.isEmpty()) {
          Text(
            text = stringResource(R.string.stats_recent_history_empty),
            style = MaterialTheme.typography.bodySmall,
            color = SecondaryGray
          )
        } else {
          Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            state.recentHistory.forEach { historyItem ->
              RecentHistoryRow(
                item = historyItem,
                onClick = { onEpisodeClick(historyItem.episode.id) }
              )
            }
          }
        }
      }
    }
  }
}

@Composable
fun AnimeStatisticsSection(
  weeklyStats: List<DayWatchStat>
) {
  val dayLabels = listOf(
    stringResource(R.string.day_mon),
    stringResource(R.string.day_tue),
    stringResource(R.string.day_wed),
    stringResource(R.string.day_thu),
    stringResource(R.string.day_fri),
    stringResource(R.string.day_sat),
    stringResource(R.string.day_sun)
  )

  val hasAnyActivity = weeklyStats.any { it.minutes > 0f }
  val modelProducer = remember { CartesianChartModelProducer() }

  LaunchedEffect(weeklyStats) {
    modelProducer.runTransaction {
      columnSeries {
        series(weeklyStats.map { it.minutes.toDouble() })
      }
    }
  }

  val bottomFormatter = remember(dayLabels) {
    CartesianValueFormatter { _, value, _ ->
      val idx = value.roundToInt().coerceIn(0, dayLabels.lastIndex)
      dayLabels[idx]
    }
  }

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("anime_statistics_section"),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
      Text(
        text = stringResource(R.string.home_anime_statistics_title),
        style = MaterialTheme.typography.titleMedium,
        color = TextPrimary
      )
      Text(
        text = stringResource(R.string.home_anime_statistics_subtitle),
        style = MaterialTheme.typography.bodySmall,
        color = SecondaryGray
      )
    }

    if (!hasAnyActivity) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(96.dp),
        contentAlignment = Alignment.Center
      ) {
        Text(
          text = stringResource(R.string.home_statistics_no_data),
          style = MaterialTheme.typography.bodySmall,
          color = SecondaryGray,
          textAlign = TextAlign.Center
        )
      }
    } else {
      ProvideVicoTheme(theme = rememberM3VicoTheme()) {
        CartesianChartHost(
          chart = rememberCartesianChart(
            rememberColumnCartesianLayer(
              columnProvider = ColumnCartesianLayer.ColumnProvider.series(
                rememberLineComponent(
                  fill = Fill(VlcOrange),
                  thickness = 16.dp
                )
              )
            ),
            startAxis = VerticalAxis.rememberStart(),
            bottomAxis = HorizontalAxis.rememberBottom(
              valueFormatter = bottomFormatter
            )
          ),
          modelProducer = modelProducer,
          modifier = Modifier
            .fillMaxWidth()
            .height(170.dp)
        )
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        weeklyStats.forEachIndexed { index, stat ->
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
              text = dayLabels.getOrElse(index) { "" },
              style = MaterialTheme.typography.labelSmall,
              color = SecondaryGray
            )
            Text(
              text = "${stat.minutes.roundToInt()}m",
              style = MaterialTheme.typography.labelSmall,
              color = if (stat.minutes > 0f) TextPrimary else SecondaryGray.copy(alpha = 0.6f)
            )
          }
        }
      }
    }
  }
}

@Composable
private fun AchievementsSection(
  achievements: List<MilestoneInfo>,
  completedAnimeCount: Int
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("stats_achievements_section"),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = stringResource(R.string.stats_achievements_title),
        style = MaterialTheme.typography.titleMedium,
        color = TextPrimary
      )
      Text(
        text = "${stringResource(R.string.stats_completed_series_label)}: $completedAnimeCount",
        style = MaterialTheme.typography.labelSmall,
        color = SecondaryGray
      )
    }

    if (achievements.any { it.isUnlocked }) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Image(
          painter = painterResource(id = R.drawable.img_achievement_celebration),
          contentDescription = stringResource(R.string.cd_celebration_illustration),
          contentScale = ContentScale.Crop,
          modifier = Modifier
            .size(width = 64.dp, height = 48.dp)
            .clip(AnimePlayRoleShapes.Poster)
        )
        Text(
          text = stringResource(R.string.celebration_title),
          style = MaterialTheme.typography.titleSmall,
          color = TextPrimary
        )
      }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
      achievements.forEach { milestone ->
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          Icon(
            imageVector = if (milestone.isUnlocked) Icons.Rounded.CheckCircle else Icons.Rounded.Lock,
            contentDescription = null,
            tint = if (milestone.isUnlocked) VlcOrange else SecondaryGray,
            modifier = Modifier.size(20.dp)
          )
          Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
              text = stringResource(milestone.titleRes),
              style = MaterialTheme.typography.titleSmall,
              color = TextPrimary
            )
            Text(
              text = stringResource(milestone.descRes),
              style = MaterialTheme.typography.bodySmall,
              color = SecondaryGray
            )
          }
          Text(
            text = stringResource(
              if (milestone.isUnlocked) R.string.milestone_unlocked_badge else R.string.milestone_locked_badge
            ),
            style = MaterialTheme.typography.labelSmall,
            color = if (milestone.isUnlocked) TextPrimary else SecondaryGray
          )
        }
      }
    }
  }
}

@Composable
private fun MalStatusBreakdownSection(
  totalCount: Int,
  breakdown: Map<String, Int>
) {
  val statuses = listOf(
    "watching" to R.string.mal_status_watching,
    "completed" to R.string.mal_status_completed,
    "plan_to_watch" to R.string.mal_status_plan,
    "on_hold" to R.string.mal_status_on_hold,
    "dropped" to R.string.mal_status_dropped
  )
  Column(
    modifier = Modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    Text(
      text = stringResource(R.string.stats_mal_breakdown_title),
      style = MaterialTheme.typography.titleMedium,
      color = TextPrimary
    )
    statuses.forEach { (key, labelRes) ->
      val count = breakdown[key] ?: 0
      val fraction = (count.toFloat() / totalCount.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text(
            text = stringResource(labelRes),
            style = MaterialTheme.typography.labelSmall,
            color = SecondaryGray
          )
          Text(
            text = count.toString(),
            style = MaterialTheme.typography.labelSmall,
            color = if (count > 0) TextPrimary else SecondaryGray
          )
        }
        LinearProgressIndicator(
          progress = { fraction },
          color = VlcOrange,
          trackColor = ObsidianSurfaceElevated,
          modifier = Modifier
            .fillMaxWidth()
            .height(5.dp)
            .clip(RoundedCornerShape(3.dp))
        )
      }
    }
  }
}

@Composable
private fun RecentHistoryRow(
  item: EpisodeWithAnime,
  onClick: () -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
      .padding(vertical = 4.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    VideoThumbnail(
      uri = item.episode.fileUri,
      contentDescription = stringResource(R.string.cd_anime_thumbnail, item.anime.title),
      modifier = Modifier
        .size(width = 80.dp, height = 48.dp)
        .clip(AnimePlayRoleShapes.Poster),
      targetWidthPx = 240,
      targetHeightPx = 150
    )
    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
      Text(
        text = item.anime.title,
        style = MaterialTheme.typography.titleSmall,
        color = TextPrimary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
      Text(
        text = "${stringResource(R.string.detail_episode_label, item.episode.number)} • ${
          TimeFormatters.formatDuration(item.episode.lastPositionMs)
        }",
        style = MaterialTheme.typography.labelSmall,
        color = SecondaryGray
      )
    }
    Icon(
      imageVector = Icons.Rounded.PlayArrow,
      contentDescription = stringResource(R.string.player_play),
      tint = VlcOrange
    )
  }
}

