package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.PlaylistAnimeCrossRef
import com.example.data.local.entity.PlaylistEntity
import com.example.data.local.entity.PlaylistSummary
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {
  @Query(
    """
    SELECT p.id, p.name, p.createdAt, COUNT(pa.animeId) AS animeCount
    FROM playlists p
    LEFT JOIN playlist_anime pa ON p.id = pa.playlistId
    GROUP BY p.id
    ORDER BY p.createdAt DESC
    """
  )
  fun observePlaylists(): Flow<List<PlaylistSummary>>

  @Query("SELECT playlistId FROM playlist_anime WHERE animeId = :animeId")
  fun observePlaylistIdsForAnime(animeId: Long): Flow<List<Long>>

  @Query("SELECT animeId FROM playlist_anime WHERE playlistId = :playlistId")
  fun observeAnimeIdsInPlaylist(playlistId: Long): Flow<List<Long>>

  @Query("SELECT * FROM playlist_anime")
  fun observeAllPlaylistMemberships(): Flow<List<PlaylistAnimeCrossRef>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertPlaylist(playlist: PlaylistEntity): Long

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun addAnimeToPlaylist(crossRef: PlaylistAnimeCrossRef)

  @Query("DELETE FROM playlist_anime WHERE playlistId = :playlistId AND animeId = :animeId")
  suspend fun removeAnimeFromPlaylist(playlistId: Long, animeId: Long)

  @Query("DELETE FROM playlists WHERE id = :playlistId")
  suspend fun deletePlaylist(playlistId: Long)
}
