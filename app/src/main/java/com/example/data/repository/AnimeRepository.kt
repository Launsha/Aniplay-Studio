package com.example.data.repository

import com.example.data.local.dao.AnimeDao
import com.example.data.local.dao.EpisodeDao
import com.example.data.local.entity.AnimeEntity
import com.example.data.local.entity.AnimeWithStats
import com.example.data.local.entity.EpisodeEntity
import com.example.data.local.entity.EpisodeWithAnime
import com.example.data.scanner.MediaStoreScanner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

data class ScanSummary(
  val animeCount: Int,
  val episodeCount: Int
)

@Singleton
class AnimeRepository @Inject constructor(
  private val animeDao: AnimeDao,
  private val episodeDao: EpisodeDao,
  private val scanner: MediaStoreScanner
) {

  fun observeLibrary(): Flow<List<AnimeWithStats>> = animeDao.observeAllAnimeWithStats()

  fun observeContinueWatching(): Flow<List<EpisodeWithAnime>> = episodeDao.observeContinueWatching()

  fun observeAnime(animeId: Long): Flow<AnimeEntity?> = animeDao.observeAnimeById(animeId)

  fun observeEpisodes(animeId: Long): Flow<List<EpisodeEntity>> = episodeDao.observeEpisodesForAnime(animeId)

  suspend fun getEpisodeWithAnime(episodeId: Long): EpisodeWithAnime? = withContext(Dispatchers.IO) {
    episodeDao.getEpisodeWithAnime(episodeId)
  }

  suspend fun getNextEpisode(animeId: Long, currentNumber: Int): EpisodeEntity? = withContext(Dispatchers.IO) {
    episodeDao.getNextEpisode(animeId, currentNumber)
  }

  suspend fun scanDownloadFolder(): ScanSummary = withContext(Dispatchers.IO) {
    val scannedFiles = scanner.scanDownloadVideos()
    val existingEpisodesByUri = episodeDao.getAllSync().associateBy { it.fileUri }
    val existingAnimeByTitle = animeDao.getAllSync().associateBy { it.title.lowercase() }

    val groupedByTitle = scannedFiles.groupBy { it.parsedInfo.title }
    val activeUris = mutableSetOf<String>()

    for ((title, files) in groupedByTitle) {
      val sortedFiles = files.sortedWith(
        compareBy({ it.parsedInfo.episodeNumber }, { it.fileName })
      )
      val firstFile = sortedFiles.first()
      val existingAnime = existingAnimeByTitle[title.lowercase()]
      val detectedYear = sortedFiles.firstNotNullOfOrNull { it.parsedInfo.releaseYear }
      val detectedGenre = sortedFiles
        .map { it.parsedInfo.genre }
        .firstOrNull { it != "Anime" }
        ?: "Anime"

      val animeId = if (existingAnime != null) {
        animeDao.update(
          existingAnime.copy(
            coverUri = firstFile.fileUri,
            episodeCount = sortedFiles.size,
            genre = if (existingAnime.genre == "Anime" && detectedGenre != "Anime") detectedGenre else existingAnime.genre,
            releaseYear = existingAnime.releaseYear ?: detectedYear
          )
        )
        existingAnime.id
      } else {
        animeDao.insert(
          AnimeEntity(
            title = title,
            coverUri = firstFile.fileUri,
            episodeCount = sortedFiles.size,
            genre = detectedGenre,
            releaseYear = detectedYear,
            lastPlayedAt = 0L
          )
        )
      }

      for (file in sortedFiles) {
        activeUris.add(file.fileUri)
        val existingEp = existingEpisodesByUri[file.fileUri]
        if (existingEp != null) {
          episodeDao.update(
            existingEp.copy(
              animeId = animeId,
              number = file.parsedInfo.episodeNumber,
              fileName = file.fileName,
              folderPath = file.folderPath,
              durationMs = if (file.durationMs > 0L) file.durationMs else existingEp.durationMs,
              subtitleUri = existingEp.subtitleUri ?: file.subtitleUri
            )
          )
        } else {
          episodeDao.insert(
            EpisodeEntity(
              animeId = animeId,
              number = file.parsedInfo.episodeNumber,
              fileUri = file.fileUri,
              fileName = file.fileName,
              folderPath = file.folderPath,
              durationMs = file.durationMs,
              lastPositionMs = 0L,
              isWatched = false,
              subtitleUri = file.subtitleUri,
              lastPlayedAt = 0L
            )
          )
        }
      }
    }

    val staleIds = existingEpisodesByUri.values
      .filter { it.fileUri !in activeUris }
      .map { it.id }

    if (staleIds.isNotEmpty()) {
      episodeDao.deleteByIds(staleIds)
    }
    animeDao.deleteEmptyAnime()

    ScanSummary(
      animeCount = groupedByTitle.size,
      episodeCount = scannedFiles.size
    )
  }

  suspend fun savePlaybackProgress(
    episodeId: Long,
    positionMs: Long,
    durationMs: Long
  ) = withContext(Dispatchers.IO) {
    val safePosition = positionMs.coerceAtLeast(0L)
    val safeDuration = durationMs.coerceAtLeast(0L)
    val isWatched = safeDuration > 0L && safePosition >= (safeDuration * 0.90).toLong()
    val now = System.currentTimeMillis()

    episodeDao.updatePlaybackProgress(
      episodeId = episodeId,
      positionMs = safePosition,
      durationMs = safeDuration,
      isWatched = isWatched,
      lastPlayedAt = now
    )
    episodeDao.getById(episodeId)?.let { ep ->
      animeDao.updateLastPlayedAt(ep.animeId, now)
    }
  }

  suspend fun setEpisodeWatched(episodeId: Long, isWatched: Boolean) = withContext(Dispatchers.IO) {
    episodeDao.setWatched(episodeId, isWatched)
  }

  suspend fun updateSubtitleUri(episodeId: Long, subtitleUri: String?) = withContext(Dispatchers.IO) {
    episodeDao.updateSubtitleUri(episodeId, subtitleUri)
  }

  suspend fun updateAnimeMetadata(animeId: Long, genre: String, releaseYear: Int?) = withContext(Dispatchers.IO) {
    val cleanGenre = genre.trim().ifBlank { "Anime" }
    animeDao.updateMetadata(animeId, cleanGenre, releaseYear)
  }
}
