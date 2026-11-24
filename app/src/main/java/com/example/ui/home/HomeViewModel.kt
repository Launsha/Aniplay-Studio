package com.example.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.AnimeWithStats
import com.example.data.local.entity.EpisodeWithAnime
import com.example.data.repository.AnimeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
  val isScanning: Boolean = false,
  val hasScannedOnce: Boolean = false,
  val isSearchExpanded: Boolean = false,
  val searchQuery: String = "",
  val selectedGenre: String? = null,
  val selectedYear: Int? = null,
  val availableGenres: List<String> = emptyList(),
  val availableYears: List<Int> = emptyList(),
  val continueWatching: List<EpisodeWithAnime> = emptyList(),
  val animeList: List<AnimeWithStats> = emptyList(),
  val totalLibraryCount: Int = 0,
  val scanError: String? = null
)

private data class FilterCriteria(
  val isSearchExpanded: Boolean = false,
  val query: String = "",
  val genre: String? = null,
  val year: Int? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
  private val repository: AnimeRepository
) : ViewModel() {

  private val isScanning = MutableStateFlow(false)
  private val hasScannedOnce = MutableStateFlow(false)
  private val scanError = MutableStateFlow<String?>(null)
  private val filterCriteria = MutableStateFlow(FilterCriteria())

  val uiState: StateFlow<HomeUiState> = combine(
    repository.observeLibrary(),
    repository.observeContinueWatching(),
    filterCriteria,
    isScanning,
    hasScannedOnce,
    scanError
  ) { values ->
    @Suppress("UNCHECKED_CAST")
    val library = values[0] as List<AnimeWithStats>
    @Suppress("UNCHECKED_CAST")
    val continueList = values[1] as List<EpisodeWithAnime>
    val filter = values[2] as FilterCriteria
    val scanning = values[3] as Boolean
    val scanned = values[4] as Boolean
    val error = values[5] as String?

    val genres = library
      .map { it.anime.genre }
      .distinct()
      .sorted()

    val years = library
      .mapNotNull { it.anime.releaseYear }
      .distinct()
      .sortedDescending()

    val trimmedQuery = filter.query.trim()
    val filteredAnime = library.filter { item ->
      val matchesQuery = trimmedQuery.isEmpty() ||
        item.anime.title.contains(trimmedQuery, ignoreCase = true) ||
        item.anime.genre.contains(trimmedQuery, ignoreCase = true) ||
        (item.anime.releaseYear?.toString()?.contains(trimmedQuery) == true)

      val matchesGenre = filter.genre == null ||
        item.anime.genre.equals(filter.genre, ignoreCase = true)

      val matchesYear = filter.year == null ||
        item.anime.releaseYear == filter.year

      matchesQuery && matchesGenre && matchesYear
    }

    HomeUiState(
      isScanning = scanning,
      hasScannedOnce = scanned,
      isSearchExpanded = filter.isSearchExpanded,
      searchQuery = filter.query,
      selectedGenre = filter.genre,
      selectedYear = filter.year,
      availableGenres = genres,
      availableYears = years,
      continueWatching = continueList,
      animeList = filteredAnime,
      totalLibraryCount = library.size,
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
        repository.scanDownloadFolder()
      }.onFailure { throwable ->
        scanError.value = throwable.localizedMessage ?: "Error"
      }
      hasScannedOnce.value = true
      isScanning.value = false
    }
  }

  fun toggleSearchExpanded() {
    val current = filterCriteria.value
    filterCriteria.value = if (current.isSearchExpanded) {
      FilterCriteria(isSearchExpanded = false)
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
}
