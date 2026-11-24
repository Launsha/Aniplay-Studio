package com.example.ui.navigation

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.detail.DetailScreen
import com.example.ui.guide.UserGuideScreen
import com.example.ui.home.HomeViewModel
import com.example.ui.home.MainShellScreen
import com.example.ui.player.PlayerScreen
import com.example.ui.settings.SettingsScreen
import kotlinx.serialization.Serializable

@Serializable
data object HomeRoute

@Serializable
data class DetailRoute(val animeId: Long)

@Serializable
data class PlayerRoute(val episodeId: Long, val startPositionMs: Long = -1L)

@Serializable
data object SettingsRoute

@Serializable
data object GuideRoute

@Composable
fun AnimePlayNavGraph() {
  val navController = rememberNavController()
  val sharedHomeViewModel: HomeViewModel = hiltViewModel()

  NavHost(
    navController = navController,
    startDestination = HomeRoute
  ) {
    composable<HomeRoute> {
      MainShellScreen(
        onAnimeClick = { animeId ->
          navController.navigate(DetailRoute(animeId))
        },
        onEpisodeClick = { episodeId ->
          navController.navigate(PlayerRoute(episodeId))
        },
        onOpenGuide = {
          navController.navigate(GuideRoute)
        },
        viewModel = sharedHomeViewModel
      )
    }

    composable<DetailRoute> {
      DetailScreen(
        onBack = { navController.popBackStack() },
        onEpisodeClick = { episodeId, startPos ->
          navController.navigate(PlayerRoute(episodeId, startPos))
        }
      )
    }

    composable<PlayerRoute> {
      PlayerScreen(
        onBack = { navController.popBackStack() }
      )
    }

    composable<SettingsRoute> {
      SettingsScreen(
        onBack = { navController.popBackStack() },
        onOpenGuide = { navController.navigate(GuideRoute) }
      )
    }

    composable<GuideRoute> {
      UserGuideScreen(
        onBack = { navController.popBackStack() },
        onReplayWelcome = {
          sharedHomeViewModel.replayOnboarding()
          navController.popBackStack(HomeRoute, inclusive = false)
        }
      )
    }
  }
}
