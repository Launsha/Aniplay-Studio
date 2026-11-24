package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.data.local.entity.WatchActivityEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchActivityDao {
  @Query("SELECT * FROM watch_activity ORDER BY dateKey DESC")
  fun observeAllActivity(): Flow<List<WatchActivityEntity>>

  @Query("SELECT COALESCE(SUM(watchedMs), 0) FROM watch_activity")
  fun observeTotalWatchedMs(): Flow<Long>

  @Query("SELECT * FROM watch_activity WHERE dateKey = :dateKey LIMIT 1")
  suspend fun getByDateKey(dateKey: String): WatchActivityEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsert(entity: WatchActivityEntity)

  @Transaction
  suspend fun addWatchedDuration(dateKey: String, dayOfWeek: Int, deltaMs: Long) {
    if (deltaMs <= 0L) return
    val existing = getByDateKey(dateKey)
    if (existing != null) {
      upsert(existing.copy(watchedMs = existing.watchedMs + deltaMs))
    } else {
      upsert(
        WatchActivityEntity(
          dateKey = dateKey,
          dayOfWeek = dayOfWeek,
          watchedMs = deltaMs
        )
      )
    }
  }

  @Query("DELETE FROM watch_activity")
  suspend fun clearAll()
}
