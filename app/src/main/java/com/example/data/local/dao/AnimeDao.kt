package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.AnimeEntity
import com.example.data.local.entity.AnimeWithStats
import kotlinx.coroutines.flow.Flow

@Dao
interface AnimeDao {
  @Query(
    """
    SELECT a.*, COALESCE(SUM(CASE WHEN e.isWatched = 1 THEN 1 ELSE 0 END), 0) AS watchedCount
    FROM anime a
    LEFT JOIN episodes e ON e.animeId = a.id
    GROUP BY a.id
    ORDER BY a.lastPlayedAt DESC, a.title COLLATE NOCASE ASC
    """
  )
  fun observeAllAnimeWithStats(): Flow<List<AnimeWithStats>>

  @Query("SELECT * FROM anime WHERE id = :animeId")
  fun observeAnimeById(animeId: Long): Flow<AnimeEntity?>

  @Query("SELECT * FROM anime WHERE title = :title LIMIT 1")
  suspend fun getByTitle(title: String): AnimeEntity?

  @Query("SELECT * FROM anime")
  suspend fun getAllSync(): List<AnimeEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(anime: AnimeEntity): Long

  @Update
  suspend fun update(anime: AnimeEntity)

  @Query("UPDATE anime SET lastPlayedAt = :timestamp WHERE id = :animeId")
  suspend fun updateLastPlayedAt(animeId: Long, timestamp: Long)

  @Query("UPDATE anime SET genre = :genre, releaseYear = :releaseYear WHERE id = :animeId")
  suspend fun updateMetadata(animeId: Long, genre: String, releaseYear: Int?)

  @Query("DELETE FROM anime WHERE id NOT IN (SELECT DISTINCT animeId FROM episodes)")
  suspend fun deleteEmptyAnime()
}
