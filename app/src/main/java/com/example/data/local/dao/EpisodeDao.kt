package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.local.entity.EpisodeEntity
import com.example.data.local.entity.EpisodeWithAnime
import kotlinx.coroutines.flow.Flow

@Dao
interface EpisodeDao {
  @Transaction
  @Query(
    """
    SELECT * FROM episodes
    WHERE lastPositionMs > 0 AND isWatched = 0
    ORDER BY lastPlayedAt DESC
    LIMIT 15
    """
  )
  fun observeContinueWatching(): Flow<List<EpisodeWithAnime>>

  @Query("SELECT * FROM episodes WHERE animeId = :animeId ORDER BY number ASC, fileName ASC")
  fun observeEpisodesForAnime(animeId: Long): Flow<List<EpisodeEntity>>

  @Transaction
  @Query("SELECT * FROM episodes WHERE id = :episodeId LIMIT 1")
  suspend fun getEpisodeWithAnime(episodeId: Long): EpisodeWithAnime?

  @Query("SELECT * FROM episodes WHERE id = :episodeId LIMIT 1")
  suspend fun getById(episodeId: Long): EpisodeEntity?

  @Query("SELECT * FROM episodes WHERE animeId = :animeId AND number > :currentNumber ORDER BY number ASC LIMIT 1")
  suspend fun getNextEpisode(animeId: Long, currentNumber: Int): EpisodeEntity?

  @Query("SELECT * FROM episodes")
  suspend fun getAllSync(): List<EpisodeEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(episode: EpisodeEntity): Long

  @Update
  suspend fun update(episode: EpisodeEntity)

  @Query(
    """
    UPDATE episodes
    SET lastPositionMs = :positionMs,
        durationMs = CASE WHEN :durationMs > 0 THEN :durationMs ELSE durationMs END,
        isWatched = CASE WHEN :isWatched = 1 THEN 1 ELSE isWatched END,
        lastPlayedAt = :lastPlayedAt
    WHERE id = :episodeId
    """
  )
  suspend fun updatePlaybackProgress(
    episodeId: Long,
    positionMs: Long,
    durationMs: Long,
    isWatched: Boolean,
    lastPlayedAt: Long
  )

  @Query("UPDATE episodes SET isWatched = :isWatched WHERE id = :episodeId")
  suspend fun setWatched(episodeId: Long, isWatched: Boolean)

  @Query("UPDATE episodes SET subtitleUri = :subtitleUri WHERE id = :episodeId")
  suspend fun updateSubtitleUri(episodeId: Long, subtitleUri: String?)

  @Query("DELETE FROM episodes WHERE id IN (:ids)")
  suspend fun deleteByIds(ids: List<Long>)
}
