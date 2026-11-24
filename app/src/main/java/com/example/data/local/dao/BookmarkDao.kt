package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.EpisodeBookmarkEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BookmarkDao {
  @Query("SELECT * FROM episode_bookmarks WHERE animeId = :animeId ORDER BY episodeNumber ASC, positionMs ASC")
  fun observeBookmarksForAnime(animeId: Long): Flow<List<EpisodeBookmarkEntity>>

  @Query("SELECT * FROM episode_bookmarks WHERE episodeId = :episodeId ORDER BY positionMs ASC")
  fun observeBookmarksForEpisode(episodeId: Long): Flow<List<EpisodeBookmarkEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(bookmark: EpisodeBookmarkEntity): Long

  @Query("DELETE FROM episode_bookmarks WHERE id = :bookmarkId")
  suspend fun deleteById(bookmarkId: Long)
}
