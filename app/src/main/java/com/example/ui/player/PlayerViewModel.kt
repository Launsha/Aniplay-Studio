package com.example.ui.player

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MimeTypes
import androidx.navigation.toRoute
import com.example.data.local.entity.EpisodeEntity
import com.example.data.repository.AnimeRepository
import com.example.ui.navigation.PlayerRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlayerUiState(
  val isLoadingEpisode: Boolean = true,
  val animeId: Long = 0L,
  val animeTitle: String = "",
  val currentEpisode: EpisodeEntity? = null,
  val nextEpisode: EpisodeEntity? = null,
  val subtitleUri: String? = null,
  val subtitleMimeType: String = MimeTypes.APPLICATION_SUBRIP,
  val subtitleDisplayName: String? = null,
  val subtitlesEnabled: Boolean = true,
  val playbackSpeed: Float = 1.0f,
  val errorMessage: String? = null
)

@HiltViewModel
class PlayerViewModel @Inject constructor(
  savedStateHandle: SavedStateHandle,
  private val repository: AnimeRepository,
  private val contentResolver: ContentResolver
) : ViewModel() {

  private val initialRoute: PlayerRoute = savedStateHandle.toRoute()

  private val _uiState = MutableStateFlow(PlayerUiState())
  val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

  init {
    loadEpisode(initialRoute.episodeId)
  }

  fun loadEpisode(episodeId: Long) {
    viewModelScope.launch {
      _uiState.update { it.copy(isLoadingEpisode = true, errorMessage = null) }
      val withAnime = repository.getEpisodeWithAnime(episodeId)
      if (withAnime == null) {
        _uiState.update {
          it.copy(
            isLoadingEpisode = false,
            errorMessage = "not_found"
          )
        }
        return@launch
      }

      val episode = withAnime.episode
      val next = repository.getNextEpisode(episode.animeId, episode.number)
      val resolvedSub = episode.subtitleUri
      val subName = resolvedSub?.let { resolveDisplayName(it) }
      val subMime = resolvedSub?.let { resolveSubtitleMimeType(it, subName) }
        ?: MimeTypes.APPLICATION_SUBRIP

      _uiState.update {
        it.copy(
          isLoadingEpisode = false,
          animeId = withAnime.anime.id,
          animeTitle = withAnime.anime.title,
          currentEpisode = episode,
          nextEpisode = next,
          subtitleUri = resolvedSub,
          subtitleMimeType = subMime,
          subtitleDisplayName = subName,
          subtitlesEnabled = true,
          errorMessage = null
        )
      }
    }
  }

  fun saveProgress(positionMs: Long, durationMs: Long) {
    val episodeId = _uiState.value.currentEpisode?.id ?: return
    viewModelScope.launch {
      repository.savePlaybackProgress(
        episodeId = episodeId,
        positionMs = positionMs,
        durationMs = durationMs
      )
    }
  }

  fun toggleSubtitles(): Boolean {
    val nextEnabled = !_uiState.value.subtitlesEnabled
    _uiState.update { it.copy(subtitlesEnabled = nextEnabled) }
    return nextEnabled
  }

  fun setPlaybackSpeed(speed: Float) {
    _uiState.update { it.copy(playbackSpeed = speed) }
  }

  fun onSubtitlePicked(uriString: String) {
    val episodeId = _uiState.value.currentEpisode?.id ?: return
    val name = resolveDisplayName(uriString)
    val mime = resolveSubtitleMimeType(uriString, name)
    viewModelScope.launch {
      repository.updateSubtitleUri(episodeId, uriString)
      _uiState.update {
        it.copy(
          subtitleUri = uriString,
          subtitleDisplayName = name,
          subtitleMimeType = mime,
          subtitlesEnabled = true
        )
      }
    }
  }

  private fun resolveDisplayName(uriString: String): String {
    val uri = Uri.parse(uriString)
    if (uri.scheme == "content") {
      runCatching {
        contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
          ?.use { cursor ->
            if (cursor.moveToFirst()) {
              val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
              if (idx >= 0) return cursor.getString(idx)
            }
          }
      }
    }
    return uri.lastPathSegment?.substringAfterLast('/') ?: "subtitle.srt"
  }

  private fun resolveSubtitleMimeType(uriString: String, displayName: String?): String {
    val candidate = (displayName ?: uriString).lowercase()
    return if (candidate.endsWith(".ass") || candidate.endsWith(".ssa")) {
      MimeTypes.TEXT_SSA
    } else {
      MimeTypes.APPLICATION_SUBRIP
    }
  }
}
