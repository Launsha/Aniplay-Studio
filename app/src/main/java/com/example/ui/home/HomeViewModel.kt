package com.example.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.data.local.entity.AnimeWithStats
import com.example.data.local.entity.EpisodeWithAnime
import com.example.data.local.entity.PlaylistAnimeCrossRef
import com.example.data.local.entity.PlaylistSummary
import com.example.data.local.entity.WatchActivityEntity
import com.example.data.preferences.AppSettings
import com.example.data.preferences.AppSettingsDataStore
import com.example.data.repository.AnimeRepository
import com.example.data.scanner.MediaStoreScanner
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DayWatchStat(
  val dayOfWeek: Int, // 1=Mon .. 7=Sun
  val minutes: Float
)

enum class LibrarySortOption {
  RECENT,
  TITLE,
  MAL_SCORE,
  YEAR
}

data class MilestoneInfo(
  val id: String,
  val titleRes: Int,
  val descRes: Int,
  val isUnlocked: Boolean
)

data class HomeUiState(
  val settingsLoaded: Boolean = false,
  val hasCompletedOnboarding: Boolean = true,
  val isScanning: Boolean = false,
  val hasScannedOnce: Boolean = false,
  val isSearchExpanded: Boolean = false,
  val searchQuery: String = "",
  val selectedGenre: String? = null,
  val selectedYear: Int? = null,
  val selectedMalStatus: String? = null,
  val selectedPlaylistId: Long? = null,
  val playlists: List<PlaylistSummary> = emptyList(),
  val favoritesOnly: Boolean = false,
  val sortOption: LibrarySortOption = LibrarySortOption.RECENT,
  val isListViewMode: Boolean = false,
  val availableGenres: List<String> = emptyList(),
  val availableYears: List<Int> = emptyList(),
  val continueWatching: List<EpisodeWithAnime> = emptyList(),
  val recentHistory: List<EpisodeWithAnime> = emptyList(),
  val favoriteAnime: List<AnimeWithStats> = emptyList(),
  val allLibrary: List<AnimeWithStats> = emptyList(),
  val animeList: List<AnimeWithStats> = emptyList(),
  val totalLibraryCount: Int = 0,
  val totalWatchedEpisodes: Int = 0,
  val completedAnimeCount: Int = 0,
  val totalWatchedMs: Long = 0L,
  val watchStreakDays: Int = 0,
  val weeklyStats: List<DayWatchStat> = (1..7).map { DayWatchStat(it, 0f) },
  val malStatusBreakdown: Map<String, Int> = emptyMap(),
  val achievements: List<MilestoneInfo> = emptyList(),
  val pendingCelebration: MilestoneInfo? = null,
  val configuredFoldersLabel: String = "Download",
  val scanError: String? = null
)

private data class FilterCriteria(
  val isSearchExpanded: Boolean = false,
  val query: String = "",
  val genre: String? = null,
  val year: Int? = null,
  val malStatus: String? = null,
  val selectedPlaylistId: Long? = null,
  val favoritesOnly: Boolean = false,
  val sortOption: LibrarySortOption = LibrarySortOption.RECENT,
  val isListViewMode: Boolean = false
)

@HiltViewModel
class HomeViewModel @Inject constructor(
  private val repository: AnimeRepository,
  private val settingsDataStore: AppSettingsDataStore
) : ViewModel() {

  private val isScanning = MutableStateFlow(false)
  private val hasScannedOnce = MutableStateFlow(false)
  private val scanError = MutableStateFlow<String?>(null)
  private val filterCriteria = MutableStateFlow(FilterCriteria())

  val uiState: StateFlow<HomeUiState> = combine(
    repository.observeLibrary(),
    repository.observeContinueWatching(),
    repository.observeRecentHistory(),
    repository.observeTotalWatchedMs(),
    repository.observeWeeklyWatchActivity(),
    repository.observePlaylists(),
    repository.observeAllPlaylistMemberships(),
    settingsDataStore.settingsFlow,
    filterCriteria,
    isScanning,
    hasScannedOnce,
    scanError
  ) { values ->
    @Suppress("UNCHECKED_CAST")
    val library = values[0] as List<AnimeWithStats>
    @Suppress("UNCHECKED_CAST")
    val continueList = values[1] as List<EpisodeWithAnime>
    @Suppress("UNCHECKED_CAST")
    val historyList = values[2] as List<EpisodeWithAnime>
    val totalWatchedMs = values[3] as Long
    @Suppress("UNCHECKED_CAST")
    val activityList = values[4] as List<WatchActivityEntity>
    @Suppress("UNCHECKED_CAST")
    val playlists = values[5] as List<PlaylistSummary>
    @Suppress("UNCHECKED_CAST")
    val playlistMemberships = values[6] as List<PlaylistAnimeCrossRef>
    val settings = values[7] as AppSettings
    val filter = values[8] as FilterCriteria
    val scanning = values[9] as Boolean
    val scanned = values[10] as Boolean
    val error = values[11] as String?

    val genres = library
      .flatMap { it.anime.genre.split(",") }
      .map { it.trim() }
      .filter { it.isNotBlank() }
      .distinct()
      .sorted()

    val years = library
      .mapNotNull { it.anime.releaseYear }
      .distinct()
      .sortedDescending()

    val selectedPlaylistAnimeIds = filter.selectedPlaylistId?.let { targetPlaylistId ->
      playlistMemberships
        .filter { it.playlistId == targetPlaylistId }
        .map { it.animeId }
        .toSet()
    }

    val trimmedQuery = filter.query.trim()
    val filteredAnime = library
      .filter { item ->
        val matchesQuery = trimmedQuery.isEmpty() ||
          item.anime.title.contains(trimmedQuery, ignoreCase = true) ||
          item.anime.genre.contains(trimmedQuery, ignoreCase = true) ||
          item.anime.releaseDate.contains(trimmedQuery, ignoreCase = true) ||
          (item.anime.releaseYear?.toString()?.contains(trimmedQuery) == true)

        val matchesGenre = filter.genre == null ||
          item.anime.genre.split(",").any { it.trim().equals(filter.genre, ignoreCase = true) }

        val matchesYear = filter.year == null ||
          item.anime.releaseYear == filter.year

        val matchesStatus = filter.malStatus == null ||
          item.anime.malStatus == filter.malStatus

        val matchesPlaylist = selectedPlaylistAnimeIds == null ||
          item.anime.id in selectedPlaylistAnimeIds

        val matchesFav = !filter.favoritesOnly || item.anime.isFavorite

        matchesQuery && matchesGenre && matchesYear && matchesStatus && matchesPlaylist && matchesFav
      }
      .let { list ->
        when (filter.sortOption) {
          LibrarySortOption.RECENT -> list.sortedWith(
            compareByDescending<AnimeWithStats> { it.anime.lastPlayedAt }
              .thenBy { it.anime.title.lowercase() }
          )
          LibrarySortOption.TITLE -> list.sortedBy { it.anime.title.lowercase() }
          LibrarySortOption.MAL_SCORE -> list.sortedByDescending { it.anime.malScore ?: -1.0 }
          LibrarySortOption.YEAR -> list.sortedByDescending { it.anime.releaseYear ?: 0 }
        }
      }

    val minutesByDay = mutableMapOf<Int, Float>()
    for (entry in activityList) {
      val current = minutesByDay[entry.dayOfWeek] ?: 0f
      minutesByDay[entry.dayOfWeek] = current + (entry.watchedMs / 60_000f)
    }
    val weeklyStats = (1..7).map { day ->
      DayWatchStat(dayOfWeek = day, minutes = minutesByDay[day] ?: 0f)
    }

    val totalWatchedEpisodes = library.sumOf { it.watchedCount }
    val completedAnimeCount = library.count {
      it.anime.episodeCount > 0 && it.watchedCount >= it.anime.episodeCount
    }

    val achievements = listOf(
      MilestoneInfo(
        id = "first_watch",
        titleRes = R.string.milestone_first_watch_title,
        descRes = R.string.milestone_first_watch_desc,
        isUnlocked = totalWatchedEpisodes >= 1
      ),
      MilestoneInfo(
        id = "hours_1",
        titleRes = R.string.milestone_one_hour_title,
        descRes = R.string.milestone_one_hour_desc,
        isUnlocked = totalWatchedMs >= 3_600_000L
      ),
      MilestoneInfo(
        id = "hours_5",
        titleRes = R.string.milestone_five_hours_title,
        descRes = R.string.milestone_five_hours_desc,
        isUnlocked = totalWatchedMs >= 5 * 3_600_000L
      ),
      MilestoneInfo(
        id = "series_complete_1",
        titleRes = R.string.milestone_series_complete_title,
        descRes = R.string.milestone_series_complete_desc,
        isUnlocked = completedAnimeCount >= 1
      )
    )

    val pendingCelebration = if (settings.hasCompletedOnboarding) {
      achievements.firstOrNull { it.isUnlocked && it.id !in settings.celebratedMilestones }
    } else {
      null
    }

    val malBreakdown = library.groupingBy { it.anime.malStatus }.eachCount()

    val foldersSummary = (settings.scanFolders.toList() + settings.scanTreeUris.map {
      MediaStoreScanner.extractDisplayFolderLabel(it)
    }).distinct().joinToString(", ").ifBlank { "-" }

    val watchStreakDays = repository.computeWatchStreakDays(activityList)

    HomeUiState(
      settingsLoaded = true,
      hasCompletedOnboarding = settings.hasCompletedOnboarding,
      isScanning = scanning,
      hasScannedOnce = scanned,
      isSearchExpanded = filter.isSearchExpanded,
      searchQuery = filter.query,
      selectedGenre = filter.genre,
      selectedYear = filter.year,
      selectedMalStatus = filter.malStatus,
      selectedPlaylistId = filter.selectedPlaylistId,
      playlists = playlists,
      favoritesOnly = filter.favoritesOnly,
      sortOption = filter.sortOption,
      isListViewMode = filter.isListViewMode,
      availableGenres = genres,
      availableYears = years,
      continueWatching = continueList,
      recentHistory = historyList,
      favoriteAnime = library.filter { it.anime.isFavorite },
      allLibrary = library,
      animeList = filteredAnime,
      totalLibraryCount = library.size,
      totalWatchedEpisodes = totalWatchedEpisodes,
      completedAnimeCount = completedAnimeCount,
      totalWatchedMs = totalWatchedMs,
      watchStreakDays = watchStreakDays,
      weeklyStats = weeklyStats,
      malStatusBreakdown = malBreakdown,
      achievements = achievements,
      pendingCelebration = pendingCelebration,
      configuredFoldersLabel = foldersSummary,
      scanError = error
    )
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = HomeUiState()
  )

  fun scanIfNeeded() {
    if (!hasScannedOnce.value && !isScanning.value) {
      refreshLibrary()
    }
  }

  fun refreshLibrary() {
    if (isScanning.value) return
    viewModelScope.launch {
      isScanning.value = true
      scanError.value = null
      runCatching {
        repository.scanConfiguredFolders()
      }.onFailure { throwable ->
        scanError.value = throwable.localizedMessage ?: "Error"
      }
      hasScannedOnce.value = true
      isScanning.value = false
    }
  }

  fun completeOnboarding() {
    viewModelScope.launch {
      settingsDataStore.setOnboardingCompleted(true)
    }
  }

  fun replayOnboarding() {
    viewModelScope.launch {
      settingsDataStore.setOnboardingCompleted(false)
    }
  }

  fun addFolderFromTreeUri(uriString: String) {
    viewModelScope.launch {
      val rel = MediaStoreScanner.extractRelativePathFromTreeUri(uriString)
      settingsDataStore.addScanFolder(
        relativePath = rel ?: "",
        treeUri = uriString
      )
      refreshLibrary()
    }
  }

  fun dismissCelebration(milestoneId: String) {
    viewModelScope.launch {
      settingsDataStore.markMilestoneCelebrated(milestoneId)
    }
  }

  fun toggleAnimeFavorite(animeId: Long, currentFavorite: Boolean) {
    viewModelScope.launch {
      repository.toggleAnimeFavorite(animeId, !currentFavorite)
    }
  }

  fun toggleSearchExpanded() {
    val current = filterCriteria.value
    filterCriteria.value = if (current.isSearchExpanded) {
      current.copy(isSearchExpanded = false, query = "")
    } else {
      current.copy(isSearchExpanded = true)
    }
  }

  fun onSearchQueryChange(query: String) {
    filterCriteria.value = filterCriteria.value.copy(query = query)
  }

  fun onSelectGenre(genre: String?) {
    val current = filterCriteria.value
    filterCriteria.value = current.copy(
      genre = if (current.genre == genre) null else genre
    )
  }

  fun onSelectYear(year: Int?) {
    val current = filterCriteria.value
    filterCriteria.value = current.copy(
      year = if (current.year == year) null else year
    )
  }

  fun onSelectMalStatus(status: String?) {
    val current = filterCriteria.value
    filterCriteria.value = current.copy(
      malStatus = if (current.malStatus == status) null else status
    )
  }

  fun toggleFavoritesOnly() {
    val current = filterCriteria.value
    filterCriteria.value = current.copy(favoritesOnly = !current.favoritesOnly)
  }

  fun setSortOption(option: LibrarySortOption) {
    filterCriteria.value = filterCriteria.value.copy(sortOption = option)
  }

  fun toggleViewMode() {
    val current = filterCriteria.value
    filterCriteria.value = current.copy(isListViewMode = !current.isListViewMode)
  }

  fun clearAllFilters() {
    filterCriteria.value = filterCriteria.value.copy(
      query = "",
      genre = null,
      year = null,
      malStatus = null,
      selectedPlaylistId = null,
      favoritesOnly = false
    )
  }

  fun onSelectPlaylist(playlistId: Long?) {
    val current = filterCriteria.value
    filterCriteria.value = current.copy(
      selectedPlaylistId = if (current.selectedPlaylistId == playlistId) null else playlistId
    )
  }

  fun createPlaylist(name: String) {
    viewModelScope.launch {
      val id = repository.createPlaylist(name)
      if (id > 0L) {
        filterCriteria.value = filterCriteria.value.copy(selectedPlaylistId = id)
      }
    }
  }

  fun deletePlaylist(playlistId: Long) {
    viewModelScope.launch {
      repository.deletePlaylist(playlistId)
      if (filterCriteria.value.selectedPlaylistId == playlistId) {
        filterCriteria.value = filterCriteria.value.copy(selectedPlaylistId = null)
      }
    }
  }
}
