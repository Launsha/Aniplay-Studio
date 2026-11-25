package com.example.ui.detail

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.example.R
import com.example.data.local.entity.AnimeEntity
import com.example.data.local.entity.EpisodeBookmarkEntity
import com.example.data.local.entity.EpisodeEntity
import com.example.data.local.entity.PlaylistSummary
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
  val playlists: List<PlaylistSummary> = emptyList(),
  val animePlaylistIds: Set<Long> = emptySet(),
  val isScanningIntroOutro: Boolean = false,
  val introOutroStatusMessage: String? = null,
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
  private val isScanningIntroOutro = MutableStateFlow(false)
  private val introOutroStatusMessage = MutableStateFlow<String?>(null)

  val uiState: StateFlow<DetailUiState> = combine(
    repository.observeAnime(route.animeId),
    repository.observeEpisodes(route.animeId),
    repository.observeBookmarksForAnime(route.animeId),
    repository.observePlaylists(),
    repository.observePlaylistIdsForAnime(route.animeId),
    malSearchState,
    isScanningIntroOutro,
    introOutroStatusMessage
  ) { values ->
    val anime = values[0] as AnimeEntity?
    @Suppress("UNCHECKED_CAST")
    val episodes = values[1] as List<EpisodeEntity>
    @Suppress("UNCHECKED_CAST")
    val bookmarks = values[2] as List<EpisodeBookmarkEntity>
    @Suppress("UNCHECKED_CAST")
    val playlists = values[3] as List<PlaylistSummary>
    @Suppress("UNCHECKED_CAST")
    val animePlaylistIds = values[4] as List<Long>
    val malSearch = values[5] as MalSearchUiState
    val scanningIntroOutro = values[6] as Boolean
    val statusMsg = values[7] as String?

    val resumeCandidate = episodes
      .filter { it.lastPositionMs > 0L && !it.isWatched }
      .maxByOrNull { it.lastPlayedAt }
      ?: episodes.firstOrNull { !it.isWatched }
      ?: episodes.firstOrNull()

    DetailUiState(
      anime = anime,
      episodes = episodes,
      bookmarks = bookmarks,
      playlists = playlists,
      animePlaylistIds = animePlaylistIds.toSet(),
      isScanningIntroOutro = scanningIntroOutro,
      introOutroStatusMessage = statusMsg,
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

  fun scanAllEpisodesIntroOutro() {
    if (isScanningIntroOutro.value) return
    viewModelScope.launch {
      isScanningIntroOutro.value = true
      val (detected, total) = runCatching {
        repository.scanIntroOutroForAnime(route.animeId)
      }.getOrDefault(0 to uiState.value.episodes.size)
      introOutroStatusMessage.value = context.getString(
        R.string.detail_scan_intro_outro_result,
        detected,
        total
      )
      isScanningIntroOutro.value = false
    }
  }

  fun consumeIntroOutroMessage() {
    introOutroStatusMessage.value = null
  }

  fun createPlaylistAndAddAnime(name: String) {
    viewModelScope.launch {
      val id = repository.createPlaylist(name)
      if (id > 0L) {
        repository.toggleAnimeInPlaylist(id, route.animeId, true)
      }
    }
  }

  fun togglePlaylistMembership(playlistId: Long, add: Boolean) {
    viewModelScope.launch {
      repository.toggleAnimeInPlaylist(playlistId, route.animeId, add)
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
