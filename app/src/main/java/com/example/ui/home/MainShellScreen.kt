package com.example.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.ui.guide.WelcomeOnboardingScreen
import com.example.ui.settings.SettingsContent
import com.example.ui.theme.ObsidianBackground
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.OnPrimaryBlack
import com.example.ui.theme.SecondaryGray
import com.example.ui.theme.VlcOrange
import com.example.ui.theme.VlcOrangeSoft

enum class MainTab(
  val titleRes: Int,
  val selectedIcon: ImageVector,
  val unselectedIcon: ImageVector,
  val testTag: String
) {
  BERANDA(R.string.nav_home, Icons.Rounded.Home, Icons.Outlined.Home, "nav_beranda"),
  PUSTAKA(R.string.nav_library, Icons.Rounded.VideoLibrary, Icons.Outlined.VideoLibrary, "nav_pustaka"),
  STATISTIK(R.string.nav_stats, Icons.Rounded.BarChart, Icons.Outlined.BarChart, "nav_statistik"),
  PENGATURAN(R.string.nav_settings, Icons.Rounded.Settings, Icons.Outlined.Settings, "nav_pengaturan")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainShellScreen(
  onAnimeClick: (Long) -> Unit,
  onEpisodeClick: (Long) -> Unit,
  onOpenGuide: () -> Unit,
  viewModel: HomeViewModel = hiltViewModel()
) {
  val state by viewModel.uiState.collectAsStateWithLifecycle()
  var currentTab by rememberSaveable { mutableStateOf(MainTab.BERANDA) }

  LaunchedEffect(state.settingsLoaded, state.hasCompletedOnboarding) {
    if (state.settingsLoaded && state.hasCompletedOnboarding) {
      viewModel.scanIfNeeded()
    }
  }

  if (state.settingsLoaded && !state.hasCompletedOnboarding) {
    WelcomeOnboardingScreen(
      onAddFolderUri = { uriString ->
        viewModel.addFolderFromTreeUri(uriString)
      },
      onComplete = {
        viewModel.completeOnboarding()
        viewModel.scanIfNeeded()
      }
    )
    return
  }

  BackHandler(enabled = currentTab != MainTab.BERANDA) {
    currentTab = MainTab.BERANDA
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

  val pageTitle = when (currentTab) {
    MainTab.BERANDA -> stringResource(R.string.app_name)
    MainTab.PUSTAKA -> stringResource(R.string.library_page_title)
    MainTab.STATISTIK -> stringResource(R.string.stats_page_title)
    MainTab.PENGATURAN -> stringResource(R.string.settings_title)
  }

  BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
    val useNavigationRail = maxWidth >= 600.dp

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
                text = pageTitle,
                style = MaterialTheme.typography.titleLarge
              )
            }
          },
          actions = {
            IconButton(
              onClick = onOpenGuide,
              modifier = Modifier
                .minimumInteractiveComponentSize()
                .testTag("guide_button")
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Rounded.HelpOutline,
                contentDescription = stringResource(R.string.action_open_guide),
                tint = MaterialTheme.colorScheme.onBackground
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
      },
      bottomBar = {
        if (!useNavigationRail) {
          NavigationBar(
            containerColor = ObsidianSurface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            windowInsets = WindowInsets.navigationBars
          ) {
            MainTab.entries.forEach { tab ->
              val selected = currentTab == tab
              NavigationBarItem(
                selected = selected,
                onClick = { currentTab = tab },
                icon = {
                  Icon(
                    imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                    contentDescription = stringResource(tab.titleRes)
                  )
                },
                label = {
                  Text(
                    text = stringResource(tab.titleRes),
                    style = MaterialTheme.typography.labelSmall
                  )
                },
                colors = NavigationBarItemDefaults.colors(
                  selectedIconColor = OnPrimaryBlack,
                  selectedTextColor = VlcOrange,
                  indicatorColor = VlcOrange,
                  unselectedIconColor = SecondaryGray,
                  unselectedTextColor = SecondaryGray
                ),
                modifier = Modifier.testTag(tab.testTag)
              )
            }
          }
        }
      }
    ) { innerPadding ->
      Row(
        modifier = Modifier
          .fillMaxSize()
          .padding(innerPadding)
      ) {
        if (useNavigationRail) {
          NavigationRail(
            containerColor = ObsidianSurface,
            modifier = Modifier
              .fillMaxHeight()
              .windowInsetsPadding(WindowInsets.navigationBars)
          ) {
            MainTab.entries.forEach { tab ->
              val selected = currentTab == tab
              NavigationRailItem(
                selected = selected,
                onClick = { currentTab = tab },
                icon = {
                  Icon(
                    imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                    contentDescription = stringResource(tab.titleRes)
                  )
                },
                label = {
                  Text(
                    text = stringResource(tab.titleRes),
                    style = MaterialTheme.typography.labelSmall
                  )
                },
                colors = NavigationRailItemDefaults.colors(
                  selectedIconColor = OnPrimaryBlack,
                  selectedTextColor = VlcOrange,
                  indicatorColor = VlcOrange,
                  unselectedIconColor = SecondaryGray,
                  unselectedTextColor = SecondaryGray
                ),
                modifier = Modifier.testTag(tab.testTag)
              )
            }
          }
        }

        Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
          if (state.isScanning) {
            LinearProgressIndicator(
              color = VlcOrange,
              trackColor = ObsidianSurface,
              modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
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

          when (currentTab) {
            MainTab.BERANDA -> BerandaTab(
              state = state,
              onAnimeClick = onAnimeClick,
              onEpisodeClick = onEpisodeClick,
              onOpenLibraryTab = { currentTab = MainTab.PUSTAKA },
              onOpenSettingsTab = { currentTab = MainTab.PENGATURAN },
              onOpenGuide = onOpenGuide,
              onRefresh = { viewModel.refreshLibrary() }
            )

            MainTab.PUSTAKA -> PustakaTab(
              state = state,
              onAnimeClick = onAnimeClick,
              onToggleFavorite = viewModel::toggleAnimeFavorite,
              onSearchQueryChange = viewModel::onSearchQueryChange,
              onSelectGenre = viewModel::onSelectGenre,
              onSelectYear = viewModel::onSelectYear,
              onToggleFavoritesOnly = viewModel::toggleFavoritesOnly,
              onSetSortOption = viewModel::setSortOption,
              onToggleViewMode = viewModel::toggleViewMode,
              onClearAllFilters = viewModel::clearAllFilters,
              onRefresh = { viewModel.refreshLibrary() },
              onOpenSettings = { currentTab = MainTab.PENGATURAN }
            )

            MainTab.STATISTIK -> StatistikTab(
              state = state,
              onEpisodeClick = onEpisodeClick
            )

            MainTab.PENGATURAN -> SettingsContent(
              onOpenGuide = onOpenGuide
            )
          }
        }
      }
    }
  }

  state.pendingCelebration?.let { milestone ->
    MilestoneCelebrationDialog(
      milestone = milestone,
      onDismiss = { viewModel.dismissCelebration(milestone.id) }
    )
  }
}

@Composable
private fun MilestoneCelebrationDialog(
  milestone: MilestoneInfo,
  onDismiss: () -> Unit
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = ObsidianSurface,
    title = {
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Image(
          painter = painterResource(id = R.drawable.img_achievement_celebration),
          contentDescription = stringResource(R.string.cd_celebration_illustration),
          contentScale = ContentScale.Crop,
          modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(4f / 3f)
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, VlcOrange.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
        )
        Text(
          text = stringResource(R.string.celebration_title),
          style = MaterialTheme.typography.titleLarge,
          color = VlcOrange,
          textAlign = TextAlign.Center
        )
      }
    },
    text = {
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Text(
          text = stringResource(milestone.titleRes),
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onSurface,
          textAlign = TextAlign.Center
        )
        Text(
          text = stringResource(milestone.descRes),
          style = MaterialTheme.typography.bodyMedium,
          color = SecondaryGray,
          textAlign = TextAlign.Center
        )
      }
    },
    confirmButton = {
      Button(
        onClick = onDismiss,
        colors = ButtonDefaults.buttonColors(
          containerColor = VlcOrange,
          contentColor = OnPrimaryBlack
        ),
        modifier = Modifier.fillMaxWidth()
      ) {
        Text(stringResource(R.string.celebration_dismiss))
      }
    }
  )
}
