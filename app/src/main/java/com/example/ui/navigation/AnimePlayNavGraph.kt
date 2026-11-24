package com.example.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.detail.DetailScreen
import com.example.ui.home.HomeScreen
import com.example.ui.player.PlayerScreen
import kotlinx.serialization.Serializable

@Serializable
data object HomeRoute

@Serializable
data class DetailRoute(val animeId: Long)

@Serializable
data class PlayerRoute(val episodeId: Long)

@Composable
fun AnimePlayNavGraph() {
  val navController = rememberNavController()

  NavHost(
    navController = navController,
    startDestination = HomeRoute
  ) {
    composable<HomeRoute> {
      HomeScreen(
        onAnimeClick = { animeId ->
          navController.navigate(DetailRoute(animeId))
        },
        onEpisodeClick = { episodeId ->
          navController.navigate(PlayerRoute(episodeId))
        }
      )
    }

    composable<DetailRoute> {
      DetailScreen(
        onBack = { navController.popBackStack() },
        onEpisodeClick = { episodeId ->
          navController.navigate(PlayerRoute(episodeId))
        }
      )
    }

    composable<PlayerRoute> {
      PlayerScreen(
        onBack = { navController.popBackStack() }
      )
    }
  }
}
