package com.example.ui.detail

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.example.data.local.entity.AnimeEntity
import com.example.data.local.entity.EpisodeBookmarkEntity
import com.example.data.local.entity.EpisodeEntity
import com.example.data.remote.MalAnimeCandidate
import com.example.data.repository.AnimeRepository
import com.example.ui.navigation.DetailRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

data class MalSearchUiState(
  val isSearching: Boolean = false,
  val results: List<MalAnimeCandidate> = emptyList(),
  val hasError: Boolean = false,
  val hasSearched: Boolean = false
)

data class DetailUiState(
  val anime: AnimeEntity? = null,
  val episodes: List<EpisodeEntity> = emptyList(),
  val bookmarks: List<EpisodeBookmarkEntity> = emptyList(),
  val nextToPlayEpisode: EpisodeEntity? = null,
  val malSearch: MalSearchUiState = MalSearchUiState()
)

@HiltViewModel
class DetailViewModel @Inject constructor(
  savedStateHandle: SavedStateHandle,
  @ApplicationContext private val context: Context,
  private val repository: AnimeRepository
) : ViewModel() {

  private val route: DetailRoute = savedStateHandle.toRoute()
  private val malSearchState = MutableStateFlow(MalSearchUiState())

  val uiState: StateFlow<DetailUiState> = combine(
    repository.observeAnime(route.animeId),
    repository.observeEpisodes(route.animeId),
    repository.observeBookmarksForAnime(route.animeId),
    malSearchState
  ) { anime, episodes, bookmarks, malSearch ->
    val resumeCandidate = episodes
      .filter { it.lastPositionMs > 0L && !it.isWatched }
      .maxByOrNull { it.lastPlayedAt }
      ?: episodes.firstOrNull { !it.isWatched }
      ?: episodes.firstOrNull()

    DetailUiState(
      anime = anime,
      episodes = episodes,
      bookmarks = bookmarks,
      nextToPlayEpisode = resumeCandidate,
      malSearch = malSearch
    )
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = DetailUiState()
  )

  fun toggleFavorite() {
    val anime = uiState.value.anime ?: return
    viewModelScope.launch {
      repository.toggleAnimeFavorite(anime.id, !anime.isFavorite)
    }
  }

  fun deleteBookmark(bookmarkId: Long) {
    viewModelScope.launch {
      repository.deleteBookmark(bookmarkId)
    }
  }

  fun toggleEpisodeWatched(episode: EpisodeEntity) {
    viewModelScope.launch {
      repository.setEpisodeWatched(episode.id, !episode.isWatched)
    }
  }

  suspend fun copyPickedCoverToInternalStorage(pickedUri: Uri): String? = withContext(Dispatchers.IO) {
    runCatching {
      val coversDir = File(context.filesDir, "covers").apply { mkdirs() }
      val targetFile = File(coversDir, "anime_${route.animeId}_${System.currentTimeMillis()}.jpg")
      context.contentResolver.openInputStream(pickedUri)?.use { input ->
        targetFile.outputStream().use { output ->
          input.copyTo(output)
        }
      }
      Uri.fromFile(targetFile).toString()
    }.getOrNull()
  }

  fun updateAnimeDetails(
    title: String,
    customCoverUri: String?,
    synopsis: String,
    genre: String,
    releaseDate: String
  ) {
    viewModelScope.launch {
      repository.updateAnimeDetails(
        animeId = route.animeId,
        title = title,
        customCoverUri = customCoverUri,
        synopsis = synopsis,
        genre = genre,
        releaseDate = releaseDate
      )
    }
  }

  fun searchMyAnimeList(query: String) {
    viewModelScope.launch {
      malSearchState.update { it.copy(isSearching = true, hasError = false, hasSearched = true) }
      val result = repository.searchMyAnimeList(query)
      result.onSuccess { candidates ->
        malSearchState.update {
          it.copy(isSearching = false, results = candidates, hasError = false)
        }
      }.onFailure {
        malSearchState.update {
          it.copy(isSearching = false, results = emptyList(), hasError = true)
        }
      }
    }
  }

  fun linkMalCandidate(candidate: MalAnimeCandidate) {
    viewModelScope.launch {
      repository.linkMyAnimeListEntry(route.animeId, candidate)
    }
  }

  fun updateMalStatus(status: String) {
    val anime = uiState.value.anime ?: return
    viewModelScope.launch {
      repository.updateMalTracking(
        animeId = anime.id,
        malId = anime.malId,
        malScore = anime.malScore,
        malStatus = status,
        malUserScore = anime.malUserScore
      )
    }
  }

  fun updateMalUserScore(score: Int?) {
    val anime = uiState.value.anime ?: return
    viewModelScope.launch {
      repository.updateMalTracking(
        animeId = anime.id,
        malId = anime.malId,
        malScore = anime.malScore,
        malStatus = anime.malStatus,
        malUserScore = score
      )
    }
  }

  fun unlinkMal() {
    val anime = uiState.value.anime ?: return
    viewModelScope.launch {
      repository.updateMalTracking(
        animeId = anime.id,
        malId = null,
        malScore = null,
        malStatus = anime.malStatus,
        malUserScore = anime.malUserScore
      )
    }
  }
}
