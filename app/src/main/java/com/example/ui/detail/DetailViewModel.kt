package com.example.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.example.data.local.entity.AnimeEntity
import com.example.data.local.entity.EpisodeEntity
import com.example.data.repository.AnimeRepository
import com.example.ui.navigation.DetailRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DetailUiState(
  val anime: AnimeEntity? = null,
  val episodes: List<EpisodeEntity> = emptyList(),
  val nextToPlayEpisode: EpisodeEntity? = null
)

@HiltViewModel
class DetailViewModel @Inject constructor(
  savedStateHandle: SavedStateHandle,
  private val repository: AnimeRepository
) : ViewModel() {

  private val route: DetailRoute = savedStateHandle.toRoute()

  val uiState: StateFlow<DetailUiState> = combine(
    repository.observeAnime(route.animeId),
    repository.observeEpisodes(route.animeId)
  ) { anime, episodes ->
    val resumeCandidate = episodes
      .filter { it.lastPositionMs > 0L && !it.isWatched }
      .maxByOrNull { it.lastPlayedAt }
      ?: episodes.firstOrNull { !it.isWatched }
      ?: episodes.firstOrNull()

    DetailUiState(
      anime = anime,
      episodes = episodes,
      nextToPlayEpisode = resumeCandidate
    )
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = DetailUiState()
  )

  fun toggleEpisodeWatched(episode: EpisodeEntity) {
    viewModelScope.launch {
      repository.setEpisodeWatched(episode.id, !episode.isWatched)
    }
  }

  fun updateAnimeMetadata(genre: String, yearInput: String) {
    val animeId = route.animeId
    val parsedYear = yearInput.trim().toIntOrNull()?.takeIf { it in 1960..2035 }
    viewModelScope.launch {
      repository.updateAnimeMetadata(animeId, genre, parsedYear)
    }
  }
}
