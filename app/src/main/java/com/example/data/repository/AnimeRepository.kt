package com.example.data.repository

import com.example.data.local.dao.AnimeDao
import com.example.data.local.dao.BookmarkDao
import com.example.data.local.dao.EpisodeDao
import com.example.data.local.dao.PlaylistDao
import com.example.data.local.dao.WatchActivityDao
import com.example.data.local.entity.AnimeEntity
import com.example.data.local.entity.AnimeWithStats
import com.example.data.local.entity.EpisodeBookmarkEntity
import com.example.data.local.entity.EpisodeEntity
import com.example.data.local.entity.EpisodeWithAnime
import com.example.data.local.entity.PlaylistAnimeCrossRef
import com.example.data.local.entity.PlaylistEntity
import com.example.data.local.entity.PlaylistSummary
import com.example.data.local.entity.WatchActivityEntity
import com.example.data.preferences.AppSettingsDataStore
import com.example.data.remote.JikanApiService
import com.example.data.remote.MalAnimeCandidate
import com.example.data.scanner.IntroOutroDetector
import com.example.data.scanner.IntroOutroSegment
import com.example.data.scanner.MediaStoreScanner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
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
  private val watchActivityDao: WatchActivityDao,
  private val bookmarkDao: BookmarkDao,
  private val playlistDao: PlaylistDao,
  private val scanner: MediaStoreScanner,
  private val introOutroDetector: IntroOutroDetector,
  private val settingsDataStore: AppSettingsDataStore,
  private val jikanApiService: JikanApiService
) {

  fun observeLibrary(): Flow<List<AnimeWithStats>> = animeDao.observeAllAnimeWithStats()

  fun observeContinueWatching(): Flow<List<EpisodeWithAnime>> = episodeDao.observeContinueWatching()

  fun observeRecentHistory(): Flow<List<EpisodeWithAnime>> = episodeDao.observeRecentHistory()

  fun observeAnime(animeId: Long): Flow<AnimeEntity?> = animeDao.observeAnimeById(animeId)

  fun observeEpisodes(animeId: Long): Flow<List<EpisodeEntity>> = episodeDao.observeEpisodesForAnime(animeId)

  fun observeBookmarksForAnime(animeId: Long): Flow<List<EpisodeBookmarkEntity>> =
    bookmarkDao.observeBookmarksForAnime(animeId)

  fun observeBookmarksForEpisode(episodeId: Long): Flow<List<EpisodeBookmarkEntity>> =
    bookmarkDao.observeBookmarksForEpisode(episodeId)

  fun observePlaylists(): Flow<List<PlaylistSummary>> = playlistDao.observePlaylists()

  fun observePlaylistIdsForAnime(animeId: Long): Flow<List<Long>> =
    playlistDao.observePlaylistIdsForAnime(animeId)

  fun observeAllPlaylistMemberships(): Flow<List<PlaylistAnimeCrossRef>> =
    playlistDao.observeAllPlaylistMemberships()

  fun observeTotalWatchedMs(): Flow<Long> = watchActivityDao.observeTotalWatchedMs()

  fun observeWeeklyWatchActivity(): Flow<List<WatchActivityEntity>> = watchActivityDao.observeAllActivity()

  suspend fun getEpisodeWithAnime(episodeId: Long): EpisodeWithAnime? = withContext(Dispatchers.IO) {
    episodeDao.getEpisodeWithAnime(episodeId)
  }

  suspend fun getNextEpisode(animeId: Long, currentNumber: Int): EpisodeEntity? = withContext(Dispatchers.IO) {
    episodeDao.getNextEpisode(animeId, currentNumber)
  }

  suspend fun scanConfiguredFolders(): ScanSummary = withContext(Dispatchers.IO) {
    val settings = settingsDataStore.settingsFlow.first()
    val scannedFiles = scanner.scanConfiguredFolders(
      allowedFolders = settings.scanFolders,
      treeUris = settings.scanTreeUris
    )

    val existingEpisodesByUri = episodeDao.getAllSync().associateBy { it.fileUri }
    val existingAnimeAll = animeDao.getAllSync()
    val existingAnimeByTitle = existingAnimeAll.associateBy { it.title.lowercase() }

    // Map existing episode -> anime so user-renamed titles keep matching their files
    val existingAnimeById = existingAnimeAll.associateBy { it.id }

    val groupedByTitle = scannedFiles.groupBy { file ->
      val existingEp = existingEpisodesByUri[file.fileUri]
      val existingAnime = existingEp?.let { existingAnimeById[it.animeId] }
      if (existingAnime != null && existingAnime.isUserEdited) {
        existingAnime.title
      } else {
        file.parsedInfo.title
      }
    }

    val activeUris = mutableSetOf<String>()
    val animeIdsToEnrich = mutableListOf<Pair<Long, String>>()

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
      val fallbackReleaseDate = detectedYear?.toString().orEmpty()

      val animeId = if (existingAnime != null) {
        if (existingAnime.isUserEdited) {
          animeDao.update(
            existingAnime.copy(
              coverUri = firstFile.fileUri,
              episodeCount = sortedFiles.size
            )
          )
        } else {
          animeDao.update(
            existingAnime.copy(
              coverUri = firstFile.fileUri,
              episodeCount = sortedFiles.size,
              genre = if (existingAnime.genre == "Anime" && detectedGenre != "Anime") detectedGenre else existingAnime.genre,
              releaseYear = existingAnime.releaseYear ?: detectedYear,
              releaseDate = existingAnime.releaseDate.ifBlank { fallbackReleaseDate }
            )
          )
          if (settings.autoEnrichMal && existingAnime.malId == null) {
            animeIdsToEnrich.add(existingAnime.id to existingAnime.title)
          }
        }
        existingAnime.id
      } else {
        val insertedId = animeDao.insert(
          AnimeEntity(
            title = title,
            coverUri = firstFile.fileUri,
            episodeCount = sortedFiles.size,
            genre = detectedGenre,
            releaseYear = detectedYear,
            releaseDate = fallbackReleaseDate,
            lastPlayedAt = 0L
          )
        )
        if (settings.autoEnrichMal) {
          animeIdsToEnrich.add(insertedId to title)
        }
        insertedId
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

    // Fast MKV chapter + ASS/SRT OP/ED detection on unscanned episodes
    if (settings.autoScanIntroOutro) {
      val currentEpisodes = episodeDao.getAllSync()
      for (ep in currentEpisodes) {
        if (ep.introOutroSource.isBlank()) {
          runCatching {
            val detected = introOutroDetector.detectFastMetadataOnly(ep)
            if (detected.hasAny) {
              episodeDao.updateIntroOutro(
                episodeId = ep.id,
                introStartMs = detected.introStartMs,
                introEndMs = detected.introEndMs,
                outroStartMs = detected.outroStartMs,
                outroEndMs = detected.outroEndMs,
                source = detected.source
              )
            }
          }
        }
      }
    }

    // Best-effort auto-fill from MyAnimeList (Jikan v4) without failing offline scans
    for ((animeId, queryTitle) in animeIdsToEnrich.take(8)) {
      runCatching {
        val candidate = searchMyAnimeListInternal(queryTitle).firstOrNull()
        if (candidate != null) {
          val current = animeDao.getById(animeId)
          if (current != null && !current.isUserEdited) {
            animeDao.update(
              current.copy(
                title = current.title, // preserve grouping key
                customCoverUri = current.customCoverUri ?: candidate.coverUrl,
                synopsis = current.synopsis.ifBlank { candidate.synopsis },
                genre = if (current.genre == "Anime" && candidate.genre.isNotBlank()) candidate.genre else current.genre,
                releaseDate = current.releaseDate.ifBlank { candidate.releaseDate },
                releaseYear = current.releaseYear ?: candidate.releaseYear,
                malId = candidate.malId,
                malScore = candidate.score
              )
            )
          }
        }
        delay(350L)
      }
    }

    ScanSummary(
      animeCount = groupedByTitle.size,
      episodeCount = scannedFiles.size
    )
  }

  suspend fun savePlaybackProgress(
    episodeId: Long,
    positionMs: Long,
    durationMs: Long,
    watchedDeltaMs: Long = 0L
  ) = withContext(Dispatchers.IO) {
    val settings = settingsDataStore.settingsFlow.first()
    val safePosition = positionMs.coerceAtLeast(0L)
    val safeDuration = durationMs.coerceAtLeast(0L)
    val thresholdRatio = settings.watchedThresholdPercent.coerceIn(70, 99) / 100.0
    val isWatched = safeDuration > 0L && safePosition >= (safeDuration * thresholdRatio).toLong()
    val now = System.currentTimeMillis()

    val previous = episodeDao.getById(episodeId)
    val deltaToRecord = if (watchedDeltaMs > 0L) {
      watchedDeltaMs.coerceAtMost(60_000L)
    } else if (previous != null) {
      val diff = safePosition - previous.lastPositionMs
      if (diff in 1L..25_000L) diff else 0L
    } else {
      0L
    }

    if (deltaToRecord > 0L && settings.saveWatchHistory) {
      val (dateKey, dayOfWeek) = todayDateAndDayOfWeek()
      watchActivityDao.addWatchedDuration(dateKey, dayOfWeek, deltaToRecord)
    }

    val recordedTimestamp = if (settings.saveWatchHistory) now else 0L
    episodeDao.updatePlaybackProgress(
      episodeId = episodeId,
      positionMs = safePosition,
      durationMs = safeDuration,
      isWatched = isWatched,
      lastPlayedAt = recordedTimestamp
    )
    previous?.let { ep ->
      if (settings.saveWatchHistory) {
        animeDao.updateLastPlayedAt(ep.animeId, now)
      }
      if (isWatched) {
        syncMalAutoStatus(ep.animeId)
      }
    }
  }

  suspend fun setEpisodeWatched(episodeId: Long, isWatched: Boolean) = withContext(Dispatchers.IO) {
    episodeDao.setWatched(episodeId, isWatched)
    episodeDao.getById(episodeId)?.let { syncMalAutoStatus(it.animeId) }
  }

  private suspend fun syncMalAutoStatus(animeId: Long) {
    val anime = animeDao.getById(animeId) ?: return
    val episodes = episodeDao.observeEpisodesForAnime(animeId).first()
    if (episodes.isNotEmpty() && episodes.all { it.isWatched } && anime.malStatus == "watching") {
      animeDao.updateMalTracking(
        animeId = animeId,
        malId = anime.malId,
        malScore = anime.malScore,
        malStatus = "completed",
        malUserScore = anime.malUserScore
      )
    }
  }

  suspend fun updateSubtitleUri(episodeId: Long, subtitleUri: String?) = withContext(Dispatchers.IO) {
    episodeDao.updateSubtitleUri(episodeId, subtitleUri)
  }

  suspend fun toggleAnimeFavorite(animeId: Long, isFavorite: Boolean) = withContext(Dispatchers.IO) {
    animeDao.setFavorite(animeId, isFavorite)
  }

  suspend fun addBookmark(
    episodeId: Long,
    animeId: Long,
    episodeNumber: Int,
    positionMs: Long,
    label: String
  ) = withContext(Dispatchers.IO) {
    bookmarkDao.insert(
      EpisodeBookmarkEntity(
        episodeId = episodeId,
        animeId = animeId,
        episodeNumber = episodeNumber,
        positionMs = positionMs.coerceAtLeast(0L),
        label = label.trim()
      )
    )
  }

  suspend fun deleteBookmark(bookmarkId: Long) = withContext(Dispatchers.IO) {
    bookmarkDao.deleteById(bookmarkId)
  }

  suspend fun scanIntroOutroForEpisode(episodeId: Long): IntroOutroSegment = withContext(Dispatchers.IO) {
    val episode = episodeDao.getById(episodeId) ?: return@withContext IntroOutroSegment()
    val siblings = episodeDao.observeEpisodesForAnime(episode.animeId).first()
    val sibling = siblings.firstOrNull { it.id != episode.id }
    val segment = introOutroDetector.detectFull(episode, sibling)
    if (segment.hasAny) {
      episodeDao.updateIntroOutro(
        episodeId = episode.id,
        introStartMs = segment.introStartMs,
        introEndMs = segment.introEndMs,
        outroStartMs = segment.outroStartMs,
        outroEndMs = segment.outroEndMs,
        source = segment.source
      )
    }
    segment
  }

  suspend fun scanIntroOutroForAnime(animeId: Long): Pair<Int, Int> = withContext(Dispatchers.IO) {
    val episodes = episodeDao.observeEpisodesForAnime(animeId).first()
    if (episodes.isEmpty()) return@withContext 0 to 0
    var detectedCount = 0
    for (idx in episodes.indices) {
      val ep = episodes[idx]
      if (ep.introOutroSource == "manual") {
        detectedCount++
        continue
      }
      val sibling = episodes.getOrNull(if (idx == 0) 1 else 0)
      val seg = runCatching { introOutroDetector.detectFull(ep, sibling) }.getOrNull()
      if (seg != null && seg.hasAny) {
        episodeDao.updateIntroOutro(
          episodeId = ep.id,
          introStartMs = seg.introStartMs,
          introEndMs = seg.introEndMs,
          outroStartMs = seg.outroStartMs,
          outroEndMs = seg.outroEndMs,
          source = seg.source
        )
        detectedCount++
      }
    }
    detectedCount to episodes.size
  }

  suspend fun saveIntroOutro(
    episodeId: Long,
    animeId: Long,
    introStartMs: Long,
    introEndMs: Long,
    outroStartMs: Long,
    outroEndMs: Long,
    source: String,
    applyToAllEpisodes: Boolean
  ) = withContext(Dispatchers.IO) {
    if (applyToAllEpisodes) {
      episodeDao.updateIntroOutroForAnime(
        animeId = animeId,
        introStartMs = introStartMs,
        introEndMs = introEndMs,
        outroStartMs = outroStartMs,
        outroEndMs = outroEndMs,
        source = source
      )
    } else {
      episodeDao.updateIntroOutro(
        episodeId = episodeId,
        introStartMs = introStartMs,
        introEndMs = introEndMs,
        outroStartMs = outroStartMs,
        outroEndMs = outroEndMs,
        source = source
      )
    }
  }

  suspend fun createPlaylist(name: String): Long = withContext(Dispatchers.IO) {
    val clean = name.trim()
    if (clean.isBlank()) return@withContext -1L
    playlistDao.insertPlaylist(PlaylistEntity(name = clean))
  }

  suspend fun deletePlaylist(playlistId: Long) = withContext(Dispatchers.IO) {
    playlistDao.deletePlaylist(playlistId)
  }

  suspend fun toggleAnimeInPlaylist(playlistId: Long, animeId: Long, add: Boolean) = withContext(Dispatchers.IO) {
    if (add) {
      playlistDao.addAnimeToPlaylist(PlaylistAnimeCrossRef(playlistId = playlistId, animeId = animeId))
    } else {
      playlistDao.removeAnimeFromPlaylist(playlistId = playlistId, animeId = animeId)
    }
  }

  fun computeWatchStreakDays(activities: List<WatchActivityEntity>): Int {
    val activeDates = activities.filter { it.watchedMs > 0L }.map { it.dateKey }.toSet()
    if (activeDates.isEmpty()) return 0
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val cal = Calendar.getInstance()
    val todayKey = sdf.format(Date(cal.timeInMillis))
    if (!activeDates.contains(todayKey)) {
      // Allow active streak up to yesterday if user hasn't watched yet today
      cal.add(Calendar.DAY_OF_YEAR, -1)
    }
    var streak = 0
    while (activeDates.contains(sdf.format(Date(cal.timeInMillis)))) {
      streak++
      cal.add(Calendar.DAY_OF_YEAR, -1)
    }
    return streak
  }

  suspend fun updateAnimeDetails(
    animeId: Long,
    title: String,
    customCoverUri: String?,
    synopsis: String,
    genre: String,
    releaseDate: String
  ) = withContext(Dispatchers.IO) {
    val cleanTitle = title.trim().ifBlank { "Anime" }
    val cleanGenre = genre.trim().ifBlank { "Anime" }
    val cleanDate = releaseDate.trim()
    val extractedYear = Regex("""\b(19[6-9]\d|20[0-3]\d)\b""")
      .find(cleanDate)
      ?.groupValues
      ?.get(1)
      ?.toIntOrNull()

    animeDao.updateEditableDetails(
      animeId = animeId,
      title = cleanTitle,
      customCoverUri = customCoverUri?.takeIf { it.isNotBlank() },
      synopsis = synopsis.trim(),
      genre = cleanGenre,
      releaseDate = cleanDate,
      releaseYear = extractedYear
    )
  }

  suspend fun searchMyAnimeList(query: String): Result<List<MalAnimeCandidate>> = withContext(Dispatchers.IO) {
    runCatching {
      searchMyAnimeListInternal(query)
    }
  }

  private suspend fun searchMyAnimeListInternal(query: String): List<MalAnimeCandidate> {
    val trimmed = query.trim()
    if (trimmed.isBlank()) return emptyList()
    val response = jikanApiService.searchAnime(query = trimmed, limit = 6)
    return response.data.map { dto ->
      val genresJoined = dto.genres.joinToString(", ") { it.name }.ifBlank { "Anime" }
      val year = dto.year ?: dto.aired?.from?.take(4)?.toIntOrNull()
      val dateText = dto.aired?.string?.takeIf { it.isNotBlank() } ?: (year?.toString().orEmpty())
      MalAnimeCandidate(
        malId = dto.malId,
        title = dto.titleEnglish?.takeIf { it.isNotBlank() } ?: dto.title,
        coverUrl = dto.images?.jpg?.largeImageUrl ?: dto.images?.jpg?.imageUrl,
        synopsis = dto.synopsis.orEmpty().trim(),
        genre = genresJoined,
        releaseDate = dateText,
        releaseYear = year,
        score = dto.score
      )
    }
  }

  suspend fun linkMyAnimeListEntry(animeId: Long, candidate: MalAnimeCandidate) = withContext(Dispatchers.IO) {
    val current = animeDao.getById(animeId) ?: return@withContext
    animeDao.update(
      current.copy(
        customCoverUri = candidate.coverUrl ?: current.customCoverUri,
        synopsis = candidate.synopsis.ifBlank { current.synopsis },
        genre = candidate.genre.ifBlank { current.genre },
        releaseDate = candidate.releaseDate.ifBlank { current.releaseDate },
        releaseYear = candidate.releaseYear ?: current.releaseYear,
        malId = candidate.malId,
        malScore = candidate.score
      )
    )
  }

  suspend fun updateMalTracking(
    animeId: Long,
    malId: Int?,
    malScore: Double?,
    malStatus: String,
    malUserScore: Int?
  ) = withContext(Dispatchers.IO) {
    animeDao.updateMalTracking(
      animeId = animeId,
      malId = malId,
      malScore = malScore,
      malStatus = malStatus,
      malUserScore = malUserScore
    )
  }

  suspend fun getLibrarySnapshot(): List<AnimeWithStats> = withContext(Dispatchers.IO) {
    animeDao.getAllSync().let {
      animeDao.observeAllAnimeWithStats().first()
    }
  }

  suspend fun clearWatchStatistics() = withContext(Dispatchers.IO) {
    watchActivityDao.clearAll()
  }

  private fun todayDateAndDayOfWeek(): Pair<String, Int> {
    val calendar = Calendar.getInstance()
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val dateKey = sdf.format(Date(calendar.timeInMillis))
    // Convert Calendar.DAY_OF_WEEK (Sun=1, Mon=2..Sat=7) -> Mon=1..Sun=7
    val normalizedDay = when (calendar.get(Calendar.DAY_OF_WEEK)) {
      Calendar.MONDAY -> 1
      Calendar.TUESDAY -> 2
      Calendar.WEDNESDAY -> 3
      Calendar.THURSDAY -> 4
      Calendar.FRIDAY -> 5
      Calendar.SATURDAY -> 6
      else -> 7
    }
    return dateKey to normalizedDay
  }
}
